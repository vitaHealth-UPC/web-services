package com.tata.intakeexecution.application.commandservices;

import com.tata.intakeexecution.domain.model.commands.ConfirmIntakeByVoiceCommand;
import com.tata.intakeexecution.application.models.VoiceConfirmationResult;

public interface ConfirmIntakeByVoiceCommandService {
    VoiceConfirmationResult handle(ConfirmIntakeByVoiceCommand command);
}
