package com.tata.accessibilitypreferences.application.commandservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPolicyCommand;

public interface UpdateNotificationPolicyCommandService {
  UserPreferences handle(UpdateNotificationPolicyCommand command);
}
