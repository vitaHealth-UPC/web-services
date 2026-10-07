package com.tata.familymonitoring.interfaces.rest.transform;

import com.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.tata.familymonitoring.interfaces.rest.resources.IntakeSummaryResource;

public final class IntakeSummaryResourceFromEntityAssembler {

  private IntakeSummaryResourceFromEntityAssembler() {
  }

  public static IntakeSummaryResource toResourceFromEntity(IntakeSummary intake) {
    return new IntakeSummaryResource(
        intake.intakeId(), intake.medicationName(), intake.scheduledAt(), intake.status());
  }
}
