package com.vitahealth.tata.familymonitoring.interfaces.rest.transform;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.IntakeSummaryResource;

/** Maps observed intake evidence without calculating schedules or changing source status. */
public final class IntakeSummaryResourceFromEntityAssembler {

  private IntakeSummaryResourceFromEntityAssembler() {}

  /** Copies the source identifier, medication name, schedule and observed result. */
  public static IntakeSummaryResource toResourceFromEntity(IntakeSummary intake) {
    return new IntakeSummaryResource(
        intake.intakeId(), intake.medicationName(), intake.scheduledAt(), intake.status());
  }
}
