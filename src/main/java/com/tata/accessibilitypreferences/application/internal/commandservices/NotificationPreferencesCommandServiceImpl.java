package com.tata.accessibilitypreferences.application.internal.commandservices;
import com.tata.accessibilitypreferences.application.commandservices.NotificationPreferencesCommandService;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationChannelsCommand;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateQuietHoursCommand;
import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class NotificationPreferencesCommandServiceImpl implements NotificationPreferencesCommandService {
    private final UpdateNotificationChannelsCommandHandler channels;
    private final UpdateQuietHoursCommandHandler quietHours;
    public NotificationPreferencesCommandServiceImpl(UpdateNotificationChannelsCommandHandler channels, UpdateQuietHoursCommandHandler quietHours) {
        this.channels = channels; this.quietHours = quietHours;
    }
    @Transactional
    public UserPreferences update(UpdateNotificationChannelsCommand channelCommand, UpdateQuietHoursCommand quietHoursCommand) {
        channels.handle(channelCommand);
        return quietHours.handle(quietHoursCommand);
    }
}
