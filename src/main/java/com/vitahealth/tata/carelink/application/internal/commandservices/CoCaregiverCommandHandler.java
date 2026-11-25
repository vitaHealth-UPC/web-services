package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.internal.authorization.EnabledCaregiverAccountGuard;
import com.vitahealth.tata.carelink.application.internal.outboundservices.AdultAccessCodeGenerator;
import com.vitahealth.tata.carelink.application.internal.outboundservices.AdultDevicePort;
import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkingCodeAllocationPort;
import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkingCodeGenerator;
import com.vitahealth.tata.carelink.application.result.AdultAccessCodeResult;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.application.result.CaregiverRequestResult;
import com.vitahealth.tata.carelink.application.result.ConsentWithdrawalResult;
import com.vitahealth.tata.carelink.domain.model.aggregates.AdultAccessCode;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.events.CareLinkConsentWithdrawn;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.AdultAccessCodeRepository;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Several caregivers for one adult. A caregiver who already cares for the adult invites another
 * one with a code; the new relationship waits until the adult accepts it. The adult can also
 * remove their whole account, which ends every relationship.
 */
@Service
@Transactional
public class CoCaregiverCommandHandler {
  static final Duration INVITATION_LIFETIME = Duration.ofHours(24);
  private static final Duration REQUEST_CODE_LIFETIME = Duration.ofMinutes(15);
  private static final int MAX_ATTEMPTS = 32;

  private final CareLinkRepository links;
  private final AdultAccessCodeRepository codes;
  private final OlderAdultProfileRepository profiles;
  private final EnabledCaregiverAccountGuard caregivers;
  private final AdultAccessCodeGenerator invitationCodes;
  private final LinkingCodeGenerator linkingCodes;
  private final LinkingCodeAllocationPort allocation;
  private final AdultDevicePort devices;
  private final RegisterConsentCommandHandler consents;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /** Injects relationship and code storage, Identity boundaries and the consent workflow. */
  public CoCaregiverCommandHandler(
      CareLinkRepository links,
      AdultAccessCodeRepository codes,
      OlderAdultProfileRepository profiles,
      EnabledCaregiverAccountGuard caregivers,
      AdultAccessCodeGenerator invitationCodes,
      LinkingCodeGenerator linkingCodes,
      LinkingCodeAllocationPort allocation,
      AdultDevicePort devices,
      RegisterConsentCommandHandler consents,
      ApplicationEventPublisher events,
      Clock clock) {
    this.links = links;
    this.codes = codes;
    this.profiles = profiles;
    this.caregivers = caregivers;
    this.invitationCodes = invitationCodes;
    this.linkingCodes = linkingCodes;
    this.allocation = allocation;
    this.devices = devices;
    this.consents = consents;
    this.events = events;
    this.clock = clock;
  }

  /** A caregiver of the adult issues a code for another caregiver (replaces the unused one). */
  public AdultAccessCodeResult invite(String careLinkId, String caregiverId) {
    caregivers.requireEnabled(caregiverId);
    var link =
        links
            .findByIdForUpdate(careLinkId)
            .filter(found -> found.caregiverId().equals(caregiverId))
            .orElseThrow(() -> notFound());
    if (!link.isActive())
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CARE_LINK_REVOKED, "care link is not active");
    codes.deleteUnused(link.id(), AdultAccessCode.Kind.CAREGIVER_INVITATION);
    var now = clock.instant();
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      var value = invitationCodes.generate();
      if (codes.exists(value) || links.isCodeReserved(value)) continue;
      var saved =
          codes.save(
              AdultAccessCode.issue(
                  link.id(),
                  link.olderAdultId(),
                  caregiverId,
                  value,
                  now,
                  INVITATION_LIFETIME,
                  AdultAccessCode.Kind.CAREGIVER_INVITATION));
      return new AdultAccessCodeResult(link.id(), saved.code().value(), saved.code().expiresAt());
    }
    throw new CareLinkApplicationException(
        CareLinkApplicationException.Code.LINKING_CODE_UNAVAILABLE,
        "invitation code could not be allocated; retry the request");
  }

  /** Another caregiver uses the code: a relationship is created waiting for the adult. */
  public CareLinkResult join(String caregiverId, String code) {
    caregivers.requireEnabled(caregiverId);
    if (code == null || code.isBlank()) throw invalid();
    var invitation =
        codes
            .findByCodeForUpdate(code.strip().toUpperCase(Locale.ROOT))
            .filter(found -> found.kind() == AdultAccessCode.Kind.CAREGIVER_INVITATION)
            .orElseThrow(() -> invalid());
    var now = clock.instant();
    if (!invitation.isValid(now))
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.LINKING_CODE_EXPIRED_OR_USED,
          "invitation is expired or already used");
    var source = links.findById(invitation.careLinkId()).orElseThrow(() -> invalid());
    if (!source.isActive())
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CARE_LINK_REVOKED, "inviting link is not active");
    var adult = invitation.olderAdultId();
    profiles.findById(adult).orElseThrow(() -> invalid());
    allocation.reserveNamespace();
    allocation.releaseExpiredReservations(now);
    if (!links.findOpenMemberPairForUpdate(caregiverId, adult, now).isEmpty())
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CARE_LINK_ALREADY_EXISTS,
          "this caregiver already cares for the adult");
    invitation.redeem(now);
    codes.save(invitation);
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      var value = linkingCodes.generate();
      if (links.isCodeReserved(value) || codes.exists(value)) continue;
      var link =
          CareLink.createPending(
              caregiverId, adult, LinkingCode.issue(value, now, REQUEST_CODE_LIFETIME), now);
      link.accept(value, now);
      return CareLinkMapper.toResult(links.save(link));
    }
    throw new CareLinkApplicationException(
        CareLinkApplicationException.Code.LINKING_CODE_UNAVAILABLE,
        "request could not be allocated; retry the request");
  }

  /** Caregivers waiting for the adult's decision. */
  @Transactional(readOnly = true)
  public List<CaregiverRequestResult> requests(String olderAdultId) {
    return links.findAwaitingConsentByOlderAdult(olderAdultId).stream()
        .map(
            link ->
                new CaregiverRequestResult(
                    link.id(),
                    link.caregiverId(),
                    devices.caregiverName(link.caregiverId()).orElse(null),
                    link.updatedAt()))
        .toList();
  }

  /** The adult accepts or rejects another caregiver. */
  public CareLinkResult decide(String olderAdultId, String careLinkId, boolean accepted) {
    var link =
        links
            .findById(careLinkId)
            .filter(found -> found.olderAdultId().equals(olderAdultId))
            .filter(found -> found.status() == CareLinkStatus.AWAITING_CONSENT)
            .orElseThrow(() -> notFound());
    return consents.handleDecision(new RegisterConsentCommand(link.id(), accepted)).link();
  }

  /**
   * Removes the adult's account: every relationship ends (caregivers stop seeing them and the
   * scoped sessions are revoked) and the PIN is deleted. History stays for audit only.
   */
  public ConsentWithdrawalResult removeAccount(String olderAdultId) {
    var now = clock.instant();
    String last = null;
    for (var open : links.findOpenByOlderAdult(olderAdultId)) {
      var link = links.findByIdForUpdate(open.id()).orElseThrow(() -> notFound());
      var approvedAt = link.consent().recordedAt();
      var changed =
          link.isActive()
              ? link.withdrawConsent(olderAdultId, now)
              : link.endByCaregiver(link.caregiverId(), now);
      if (!changed) continue;
      var saved = links.save(link);
      events.publishEvent(
          new CareLinkConsentWithdrawn(
              saved.id(),
              saved.caregiverId(),
              saved.olderAdultId(),
              approvedAt == null ? now : approvedAt,
              now));
      last = saved.id();
    }
    devices.removePin(olderAdultId);
    return new ConsentWithdrawalResult(last == null ? olderAdultId : last, last != null);
  }

  private static CareLinkApplicationException notFound() {
    return new CareLinkApplicationException(
        CareLinkApplicationException.Code.CARE_LINK_NOT_FOUND, "care link not found");
  }

  private static CareLinkApplicationException invalid() {
    return new CareLinkApplicationException(
        CareLinkApplicationException.Code.INVALID_LINKING_CODE, "invitation is invalid");
  }
}
