package com.vitahealth.tata.treatmentmanagement.domain.model.commands;

/**
 * Requests an unconfigured treatment draft for an adult owned through a consented care link. A
 * {@code planId} adds the draft as another medication of an existing treatment plan.
 */
public record CreateTreatmentCommand(
    String caregiverId, String olderAdultId, String name, String planId) {
  /** Creates a standalone treatment. */
  public CreateTreatmentCommand(String caregiverId, String olderAdultId, String name) {
    this(caregiverId, olderAdultId, name, null);
  }
}
