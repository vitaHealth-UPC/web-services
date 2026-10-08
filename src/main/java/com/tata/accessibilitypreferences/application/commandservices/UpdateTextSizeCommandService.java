package com.tata.accessibilitypreferences.application.commandservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateTextSizeCommand;

public interface UpdateTextSizeCommandService {
    UserPreferences handle(UpdateTextSizeCommand command);
}
