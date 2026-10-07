package com.tata.accessibilitypreferences.application.commandservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateQuietHoursCommand;

public interface UpdateQuietHoursCommandService {
    UserPreferences handle(UpdateQuietHoursCommand command);
}
