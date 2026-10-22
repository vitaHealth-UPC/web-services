package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.internal.authorization.OwnedOlderAdultProfileGuard;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.commands.AssociateEmergencyContactCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies the profile change under an ownership check and a persistent mutation lock. */
@Service
@Transactional
public class AssociateEmergencyContactCommandHandler {
  private final OwnedOlderAdultProfileGuard ownership;
  private final OlderAdultProfileRepository profiles;
  private final Clock clock;

  /** Injects authorization, persistence and the shared update time source. */
  public AssociateEmergencyContactCommandHandler(
      OwnedOlderAdultProfileGuard ownership, OlderAdultProfileRepository profiles, Clock clock) {
    this.ownership = ownership;
    this.profiles = profiles;
    this.clock = clock;
  }

  /** Preserves registration ownership and creation evidence while updating the requested data. */
  public OlderAdultProfileResult handle(AssociateEmergencyContactCommand command) {
    var profile = ownership.requireForUpdate(command.caregiverId(), command.olderAdultId());
    profile.associateEmergencyContact(
        new EmergencyContact(command.fullName(), command.relationship(), command.phone()),
        clock.instant());
    return CareLinkMapper.toResult(profiles.save(profile));
  }
}
