package com.tata.accessibilitypreferences.application.commandservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateReadingAssistanceCommand;

public interface UpdateReadingAssistanceCommandService {
    UserPreferences handle(UpdateReadingAssistanceCommand command);
}
