package com.vitahealth.tata.familymonitoring.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Initializes follow-up for one confirmed caregiver-adult relationship. */
public record InitializeFamilyMonitorCommand(
    String careLinkId, String olderAdultId, String caregiverId) {

  /** Requires the relationship and both logical participants. */
  public InitializeFamilyMonitorCommand {
    careLinkId = requireText(careLinkId, "careLinkId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    caregiverId = requireText(caregiverId, "caregiverId");
  }
}
