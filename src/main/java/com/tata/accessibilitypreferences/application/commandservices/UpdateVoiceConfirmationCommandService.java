package com.tata.accessibilitypreferences.application.commandservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateVoiceConfirmationCommand;

public interface UpdateVoiceConfirmationCommandService {
    UserPreferences handle(UpdateVoiceConfirmationCommand command);
}
