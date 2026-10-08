package com.tata.accessibilitypreferences.interfaces.rest.transform;

import com.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationChannelsCommand;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateQuietHoursCommand;
import com.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UpdateQuietHoursResource;

public final class UpdateQuietHoursCommandFromResourceAssembler {
    private UpdateQuietHoursCommandFromResourceAssembler() {}

    /** Builds the range here so an invalid interval is rejected before anything is saved. */
    public static UpdateQuietHoursCommand toQuietHoursCommand(String userId, UpdateQuietHoursResource resource) {
        var quietHours = resource.quietHours();
        return new UpdateQuietHoursCommand(
                userId,
                quietHours == null ? null : QuietHoursRange.between(quietHours.start(), quietHours.end()));
    }

    public static UpdateNotificationChannelsCommand toChannelsCommand(String userId, UpdateQuietHoursResource resource) {
        return new UpdateNotificationChannelsCommand(
                userId,
                resource.channels().stream()
                        .map(channel -> new NotificationChannel(channel.type(), channel.enabled()))
                        .toList());
    }
}
