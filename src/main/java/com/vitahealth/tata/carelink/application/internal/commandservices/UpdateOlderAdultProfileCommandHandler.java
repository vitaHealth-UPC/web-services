package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.internal.authorization.OwnedOlderAdultProfileGuard;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.commands.UpdateOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies the profile change under an ownership check and a persistent mutation lock. */
@Service
@Transactional
public class UpdateOlderAdultProfileCommandHandler {
  private final OwnedOlderAdultProfileGuard ownership;
  private final OlderAdultProfileRepository profiles;
  private final Clock clock;

  /** Injects authorization, persistence and the shared update time source. */
  public UpdateOlderAdultProfileCommandHandler(
      OwnedOlderAdultProfileGuard ownership, OlderAdultProfileRepository profiles, Clock clock) {
    this.ownership = ownership;
    this.profiles = profiles;
    this.clock = clock;
  }

  /** Preserves registration ownership and creation evidence while updating the requested data. */
  public OlderAdultProfileResult handle(UpdateOlderAdultProfileCommand command) {
    var profile = ownership.requireForUpdate(command.caregiverId(), command.olderAdultId());
    profile.updateBasicData(
        new OlderAdultBasicData(command.fullName(), command.birthDate()), clock.instant());
    return CareLinkMapper.toResult(profiles.save(profile));
  }
}
