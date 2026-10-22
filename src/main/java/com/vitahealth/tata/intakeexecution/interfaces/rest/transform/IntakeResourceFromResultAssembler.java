package com.vitahealth.tata.intakeexecution.interfaces.rest.transform;

import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.interfaces.rest.resources.IntakeResource;

/** Maps intake evidence into its HTTP representation without application orchestration. */
public final class IntakeResourceFromResultAssembler {
  private IntakeResourceFromResultAssembler() {}

  /** Preserves stored confirmation evidence and request-specific replay metadata. */
  public static IntakeResource toResource(IntakeResult result) {
    return new IntakeResource(
        result.id(),
        result.treatmentId(),
        result.medicationId(),
        result.olderAdultId(),
        result.medicationName(),
        result.dose(),
        result.instructions(),
        result.scheduledAt(),
        result.status(),
        result.confirmedAt(),
        result.confirmationChannel(),
        result.alreadyConfirmed());
  }
}
