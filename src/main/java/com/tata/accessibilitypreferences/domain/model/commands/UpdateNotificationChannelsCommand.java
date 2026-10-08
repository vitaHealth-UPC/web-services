package com.tata.accessibilitypreferences.domain.model.commands;

import com.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import java.util.List;

public record UpdateNotificationChannelsCommand(String userId, List<NotificationChannel> channels) {}
