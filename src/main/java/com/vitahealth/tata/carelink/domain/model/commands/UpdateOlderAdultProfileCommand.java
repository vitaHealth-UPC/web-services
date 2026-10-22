package com.vitahealth.tata.carelink.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.LocalDate;

/** Requests a caregiver-owned profile change without accepting ownership reassignment. */
public record UpdateOlderAdultProfileCommand(
    String caregiverId, String olderAdultId, String fullName, LocalDate birthDate) {
  /** Requires explicit actor and target identities independently of transport. */
  public UpdateOlderAdultProfileCommand {
    caregiverId = requireText(caregiverId, "caregiverId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
  }

  /** Keeps personal profile/contact fields out of diagnostic output. */
  @Override
  public String toString() {
    return "UpdateOlderAdultProfileCommand[olderAdultId="
        + olderAdultId
        + ", personalData=REDACTED]";
  }
}
