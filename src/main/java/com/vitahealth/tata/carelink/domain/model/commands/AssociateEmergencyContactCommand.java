package com.vitahealth.tata.carelink.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Requests a caregiver-owned profile change without accepting ownership reassignment. */
public record AssociateEmergencyContactCommand(
    String caregiverId, String olderAdultId, String fullName, String relationship, String phone) {
  /** Requires explicit actor and target identities independently of transport. */
  public AssociateEmergencyContactCommand {
    caregiverId = requireText(caregiverId, "caregiverId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
  }

  /** Keeps personal profile/contact fields out of diagnostic output. */
  @Override
  public String toString() {
    return "AssociateEmergencyContactCommand[olderAdultId="
        + olderAdultId
        + ", personalData=REDACTED]";
  }
}
