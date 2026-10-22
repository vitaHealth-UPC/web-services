package com.vitahealth.tata.intakeexecution.application.commandservices;

import com.vitahealth.tata.intakeexecution.domain.model.commands.MarkIntakeOmittedCommand;

/** Applies a definitive omission through the Intake application boundary. */
public interface MarkIntakeOmittedCommandService {
  /** Changes only pending evidence and retains already definitive outcomes. */
  void handle(MarkIntakeOmittedCommand command);
}
