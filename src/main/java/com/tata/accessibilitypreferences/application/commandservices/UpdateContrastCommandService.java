package com.tata.accessibilitypreferences.application.commandservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateContrastCommand;

public interface UpdateContrastCommandService {
    UserPreferences handle(UpdateContrastCommand command);
}
