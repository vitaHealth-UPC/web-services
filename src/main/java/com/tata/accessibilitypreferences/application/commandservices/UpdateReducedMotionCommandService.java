package com.tata.accessibilitypreferences.application.commandservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateReducedMotionCommand;

public interface UpdateReducedMotionCommandService {
    UserPreferences handle(UpdateReducedMotionCommand command);
}
