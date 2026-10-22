package com.vitahealth.tata.intakeexecution.interfaces.events;

import com.vitahealth.tata.intakeexecution.application.commandservices.MarkIntakeOmittedCommandService;
import com.vitahealth.tata.intakeexecution.domain.model.commands.MarkIntakeOmittedCommand;
import com.vitahealth.tata.omissionescalation.domain.model.events.IntakeOmitted;
import org.springframework.stereotype.Component;

/** Translates the incoming IntakeOmitted contract into a public Intake command. */
@Component("intakeExecutionOmittedEventConsumer")
public class IntakeOmittedEventConsumer {
  private final MarkIntakeOmittedCommandService commands;

  /** Injects the application contract without depending on its internal handler. */
  public IntakeOmittedEventConsumer(MarkIntakeOmittedCommandService commands) {
    this.commands = commands;
  }

  /** Applies the event in the source transaction; failures propagate to the publisher. */
  public void consume(IntakeOmitted event) {
    commands.handle(new MarkIntakeOmittedCommand(event.intakeId()));
  }
}
