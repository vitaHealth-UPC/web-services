package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.internal.authorization.EnabledCaregiverAccountGuard;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers adult profile data independently of linking-code and consent workflows. */
@Service
@Transactional
public class RegisterOlderAdultProfileCommandHandler {
  private final OlderAdultProfileRepository profiles;
  private final EnabledCaregiverAccountGuard caregivers;
  private final Clock clock;

  /** Injects profile persistence, caregiver eligibility and the shared observable time source. */
  public RegisterOlderAdultProfileCommandHandler(
      OlderAdultProfileRepository profiles, EnabledCaregiverAccountGuard caregivers, Clock clock) {
    this.profiles = profiles;
    this.caregivers = caregivers;
    this.clock = clock;
  }

  /** Validates optional contact data as one value and persists the new adult profile. */
  public OlderAdultProfileResult handle(RegisterOlderAdultProfileCommand command) {
    caregivers.requireEnabled(command.caregiverId());
    EmergencyContact contact = null;
    if (notBlank(command.emergencyContactName())
        || notBlank(command.emergencyContactRelationship())
        || notBlank(command.emergencyContactPhone())) {
      contact =
          new EmergencyContact(
              command.emergencyContactName(),
              command.emergencyContactRelationship(),
              command.emergencyContactPhone());
    }
    var profile =
        OlderAdultProfile.register(
            command.caregiverId(),
            new OlderAdultBasicData(command.fullName(), command.birthDate()),
            contact,
            clock.instant());
    return CareLinkMapper.toResult(profiles.save(profile));
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }
}
