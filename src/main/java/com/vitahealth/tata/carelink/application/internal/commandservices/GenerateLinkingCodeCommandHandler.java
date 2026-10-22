package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.internal.authorization.EnabledCaregiverAccountGuard;
import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkingCodeAllocationPort;
import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkingCodeGenerator;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns caregiver/profile eligibility and issuance of pending linking evidence. */
@Service
@Transactional
public class GenerateLinkingCodeCommandHandler {
  private static final Duration CODE_LIFETIME = Duration.ofMinutes(15);
  private static final int MAX_ALLOCATION_ATTEMPTS = 64;
  private final OlderAdultProfileRepository profiles;
  private final CareLinkRepository links;
  private final EnabledCaregiverAccountGuard caregivers;
  private final LinkingCodeGenerator codes;
  private final Clock clock;
  private final LinkingCodeAllocationPort allocation;

  /** Injects the issuance dependencies without owning authentication or profile registration. */
  public GenerateLinkingCodeCommandHandler(
      OlderAdultProfileRepository profiles,
      CareLinkRepository links,
      EnabledCaregiverAccountGuard caregivers,
      LinkingCodeGenerator codes,
      Clock clock,
      LinkingCodeAllocationPort allocation) {
    this.profiles = profiles;
    this.links = links;
    this.caregivers = caregivers;
    this.codes = codes;
    this.clock = clock;
    this.allocation = allocation;
  }

  /** Reuses a live pending request or allocates new evidence after expiry or revocation. */
  public CareLinkResult handle(GenerateLinkingCodeCommand command) {
    caregivers.requireEnabled(command.caregiverId());
    var profile = profiles.findById(command.olderAdultId()).orElseThrow(() -> unavailableProfile());
    if (!profile.registeredByCaregiverId().equals(command.caregiverId()))
      throw unavailableProfile();
    allocation.reserveNamespace();
    var now = clock.instant();
    allocation.releaseExpiredReservations(now);
    var open =
        links.findOpenMemberPairForUpdate(command.caregiverId(), command.olderAdultId(), now);
    if (open.size() == 1 && open.getFirst().status() == CareLinkStatus.PENDING) {
      return CareLinkMapper.toResult(open.getFirst());
    }
    if (!open.isEmpty()) {
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CARE_LINK_ALREADY_EXISTS,
          "this caregiver and adult already have an open care relationship");
    }
    for (int attempt = 0; attempt < MAX_ALLOCATION_ATTEMPTS; attempt++) {
      var code = LinkingCode.issue(codes.generate(), now, CODE_LIFETIME);
      if (links.isCodeReserved(code.value())) continue;
      return CareLinkMapper.toResult(
          links.save(CareLink.createPending(command.caregiverId(), profile.id(), code, now)));
    }
    throw new CareLinkApplicationException(
        CareLinkApplicationException.Code.LINKING_CODE_UNAVAILABLE,
        "linking code could not be allocated; retry the request");
  }

  private static CareLinkApplicationException unavailableProfile() {
    return new CareLinkApplicationException(
        CareLinkApplicationException.Code.OLDER_ADULT_NOT_FOUND,
        "older adult profile is not available to this caregiver");
  }
}
