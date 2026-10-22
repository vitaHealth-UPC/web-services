package com.vitahealth.tata.intakeexecution.interfaces.events.transform;

import com.vitahealth.tata.intakeexecution.domain.model.commands.GenerateIntakesCommand;
import com.vitahealth.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import java.util.Objects;

/** Translates the Treatment context snapshot into the Intake schedule command contract. */
public final class GenerateIntakesCommandFromEventAssembler {
  private GenerateIntakesCommandFromEventAssembler() {}

  /** Preserves explicit schedule values without generating times or changing clinical frequency. */
  public static GenerateIntakesCommand toCommand(TreatmentScheduleChangedEvent event) {
    Objects.requireNonNull(event, "schedule event");
    return new GenerateIntakesCommand(
        event.treatmentId(),
        event.medicationId(),
        event.olderAdultId(),
        event.medicationName(),
        event.dose(),
        event.frequency(),
        event.scheduledTimes(),
        event.instructions(),
        event.reminderLeadMinutes(),
        event.active(),
        event.recurrenceMode(),
        event.recurrenceZone(),
        event.recurrenceAnchor());
  }
}
