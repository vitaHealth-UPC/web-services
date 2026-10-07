package com.tata.intakeexecution.application.commandservices;

import com.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.tata.intakeexecution.application.models.IntakeResult;

public interface ConfirmIntakeCommandService {
    IntakeResult handle(ConfirmIntakeCommand command);
}
