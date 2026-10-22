package com.vitahealth.tata.intakeexecution.application.internal.assemblers;

import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;

/** Projects immutable intake evidence and command-specific retry metadata. */
public final class IntakeResultFromAggregateAssembler {
  private IntakeResultFromAggregateAssembler() {}

  /** Projects stored evidence without implying a repeated confirmation request. */
  public static IntakeResult toResult(Intake intake) {
    return toResult(intake, false);
  }

  /** Request metadata: true only when a confirmation command made no new transition. */
  public static IntakeResult toResult(Intake intake, boolean alreadyConfirmed) {
    return new IntakeResult(
        intake.id(),
        intake.treatmentId(),
        intake.medicationId(),
        intake.olderAdultId(),
        intake.medication().name(),
        intake.medication().dose(),
        intake.medication().instructions(),
        intake.scheduledAt(),
        intake.status(),
        intake.confirmedAt(),
        intake.confirmationChannel(),
        alreadyConfirmed);
  }
}
