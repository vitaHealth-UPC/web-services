package com.tata.accessibilitypreferences.application.commandservices;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationChannelsCommand;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateQuietHoursCommand;
import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
public interface NotificationPreferencesCommandService {
    UserPreferences update(UpdateNotificationChannelsCommand channels, UpdateQuietHoursCommand quietHours);
}
