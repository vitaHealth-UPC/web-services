package com.vitahealth.tata.carelink.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.LocalDate;

/** Registers basic adult data and an optional complete emergency contact for a caregiver. */
public record RegisterOlderAdultProfileCommand(
    String caregiverId,
    String fullName,
    LocalDate birthDate,
    String emergencyContactName,
    String emergencyContactRelationship,
    String emergencyContactPhone) {
  /** Requires the registering identity before the application consults account eligibility. */
  public RegisterOlderAdultProfileCommand {
    caregiverId = requireText(caregiverId, "caregiverId");
  }

  /** Omits submitted profile and contact data from diagnostic output. */
  @Override
  public String toString() {
    return "RegisterOlderAdultProfileCommand[caregiverId="
        + caregiverId
        + ", personalData=REDACTED]";
  }
}
