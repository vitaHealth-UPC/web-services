package com.vitahealth.tata.treatmentmanagement.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Requests extension using current persisted evidence rather than a batch snapshot. */
public record ExtendTreatmentScheduleCommand(String treatmentId) {
  /** Requires the stable treatment identity. */
  public ExtendTreatmentScheduleCommand {
    treatmentId = requireText(treatmentId, "treatmentId");
  }
}
