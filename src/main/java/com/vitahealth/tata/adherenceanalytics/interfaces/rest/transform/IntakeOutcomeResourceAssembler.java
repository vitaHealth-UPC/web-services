package com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.IntakeOutcome;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.IntakeOutcomeResource;

/** Maps source evidence into its explicit REST representation. */
public final class IntakeOutcomeResourceAssembler {
  private IntakeOutcomeResourceAssembler() {}

  /** Preserves recorded evidence without recalculating domain state. */
  public static IntakeOutcomeResource from(IntakeOutcome outcome) {
    return new IntakeOutcomeResource(
        outcome.medicationId(), outcome.scheduledAt(), outcome.status().name());
  }
}
