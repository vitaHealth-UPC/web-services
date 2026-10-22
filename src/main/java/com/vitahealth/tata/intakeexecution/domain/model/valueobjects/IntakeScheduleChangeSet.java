package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import java.util.List;

/** Persistence decisions with structurally immutable lists and pending-only cancellations. */
public record IntakeScheduleChangeSet(List<Intake> pending, List<Intake> cancelled) {
  /** Copies both lists and rejects decisions that would overwrite definitive evidence. */
  public IntakeScheduleChangeSet {
    pending = List.copyOf(pending);
    cancelled = List.copyOf(cancelled);
    if (pending.stream().anyMatch(intake -> !intake.isPending())
        || cancelled.stream().anyMatch(intake -> !intake.isPending())) {
      throw new IllegalArgumentException("schedule changes may only affect pending intakes");
    }
  }
}
