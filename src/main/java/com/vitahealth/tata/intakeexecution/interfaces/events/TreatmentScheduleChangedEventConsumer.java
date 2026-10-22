package com.vitahealth.tata.intakeexecution.interfaces.events;

import com.vitahealth.tata.intakeexecution.application.commandservices.GenerateIntakesCommandService;
import com.vitahealth.tata.intakeexecution.interfaces.events.transform.GenerateIntakesCommandFromEventAssembler;
import com.vitahealth.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import org.springframework.stereotype.Component;

/** Translates the incoming TreatmentScheduleChanged contract into a public Intake command. */
@Component
public class TreatmentScheduleChangedEventConsumer {
  private final GenerateIntakesCommandService commands;

  /** Injects the application contract without depending on its internal handler. */
  public TreatmentScheduleChangedEventConsumer(GenerateIntakesCommandService commands) {
    this.commands = commands;
  }

  /** Applies the event in the source transaction; failures propagate to the publisher. */
  public void consume(TreatmentScheduleChangedEvent event) {
    commands.handle(GenerateIntakesCommandFromEventAssembler.toCommand(event));
  }
}
