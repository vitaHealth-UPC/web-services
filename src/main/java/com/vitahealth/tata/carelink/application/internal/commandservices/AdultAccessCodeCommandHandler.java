package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.internal.authorization.EnabledCaregiverAccountGuard;
import com.vitahealth.tata.carelink.application.internal.outboundservices.AdultAccessCodeGenerator;
import com.vitahealth.tata.carelink.application.internal.outboundservices.AdultDevicePort;
import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkSessionPort;
import com.vitahealth.tata.carelink.application.result.AdultAccessCodeResult;
import com.vitahealth.tata.carelink.application.result.AdultAccessRedemptionResult;
import com.vitahealth.tata.carelink.domain.model.aggregates.AdultAccessCode;
import com.vitahealth.tata.carelink.domain.model.commands.IssueAdultAccessCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RedeemAdultAccessCodeCommand;
import com.vitahealth.tata.carelink.domain.repositories.AdultAccessCodeRepository;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and redeems the codes that let an adult sign in again after reinstalling the app or
 * changing phones, without the caregiver handling the adult's device.
 */
@Service
@Transactional
public class AdultAccessCodeCommandHandler {
  /** Long enough to share by message and type on another day. */
  static final Duration LIFETIME = Duration.ofHours(24);

  private static final int MAX_ATTEMPTS = 32;
  private final CareLinkRepository links;
  private final AdultAccessCodeRepository codes;
  private final OlderAdultProfileRepository profiles;
  private final EnabledCaregiverAccountGuard caregivers;
  private final AdultAccessCodeGenerator generator;
  private final AdultDevicePort devices;
  private final LinkSessionPort sessions;
  private final Clock clock;

  /** Injects relationship, code and profile storage plus Identity boundaries. */
  public AdultAccessCodeCommandHandler(
      CareLinkRepository links,
      AdultAccessCodeRepository codes,
      OlderAdultProfileRepository profiles,
      EnabledCaregiverAccountGuard caregivers,
      AdultAccessCodeGenerator generator,
      AdultDevicePort devices,
      LinkSessionPort sessions,
      Clock clock) {
    this.links = links;
    this.codes = codes;
    this.profiles = profiles;
    this.caregivers = caregivers;
    this.generator = generator;
    this.devices = devices;
    this.sessions = sessions;
    this.clock = clock;
  }

  /** Replaces any unused code of the relationship with a new one. */
  public AdultAccessCodeResult issue(IssueAdultAccessCodeCommand command) {
    caregivers.requireEnabled(command.caregiverId());
    var link =
        links
            .findByIdForUpdate(command.careLinkId())
            .filter(found -> found.caregiverId().equals(command.caregiverId()))
            .orElseThrow(() -> notFound());
    if (!link.isActive())
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CARE_LINK_REVOKED, "care link is not active");
    codes.deleteUnusedByCareLinkId(link.id());
    var now = clock.instant();
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      var value = generator.generate();
      if (codes.exists(value) || links.isCodeReserved(value)) continue;
      var saved =
          codes.save(
              AdultAccessCode.issue(
                  link.id(), link.olderAdultId(), command.caregiverId(), value, now, LIFETIME));
      return new AdultAccessCodeResult(link.id(), saved.code().value(), saved.code().expiresAt());
    }
    throw new CareLinkApplicationException(
        CareLinkApplicationException.Code.LINKING_CODE_UNAVAILABLE,
        "access code could not be allocated; retry the request");
  }

  /**
   * Consumes the code. With a PIN the device must still sign in with it; without one, a session
   * lets the adult create it.
   */
  public AdultAccessRedemptionResult redeem(RedeemAdultAccessCodeCommand command) {
    if (command.code() == null || command.code().isBlank()) throw invalid();
    var normalized = command.code().strip().toUpperCase(Locale.ROOT);
    var code =
        codes
            .findByCodeForUpdate(normalized)
            .filter(found -> found.kind() == AdultAccessCode.Kind.ADULT_ACCESS)
            .orElseThrow(() -> invalid());
    var now = clock.instant();
    if (!code.isValid(now))
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.LINKING_CODE_EXPIRED_OR_USED,
          "access code is expired or already used");
    var link = links.findByIdForUpdate(code.careLinkId()).orElseThrow(() -> invalid());
    if (!link.isActive())
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CARE_LINK_REVOKED, "care link is not active");
    code.redeem(now);
    codes.save(code);
    var name =
        profiles
            .findById(link.olderAdultId())
            .map(profile -> profile.basicData().fullName())
            .orElse(null);
    if (devices.hasPin(link.olderAdultId()))
      return new AdultAccessRedemptionResult(
          link.id(), link.olderAdultId(), name, true, null, null);
    var session = sessions.establish(CareLinkMapper.toResult(link));
    return new AdultAccessRedemptionResult(
        link.id(), link.olderAdultId(), name, false, session.accessToken(), session.expiresAt());
  }

  private static CareLinkApplicationException notFound() {
    return new CareLinkApplicationException(
        CareLinkApplicationException.Code.CARE_LINK_NOT_FOUND, "care link not found");
  }

  private static CareLinkApplicationException invalid() {
    return new CareLinkApplicationException(
        CareLinkApplicationException.Code.INVALID_LINKING_CODE, "access code is invalid");
  }
}
