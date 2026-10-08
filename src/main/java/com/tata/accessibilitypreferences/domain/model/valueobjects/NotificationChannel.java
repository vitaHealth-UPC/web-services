package com.tata.accessibilitypreferences.domain.model.valueobjects;

import java.util.Objects;

/** A notification channel and whether the user allows it. */
public record NotificationChannel(ChannelType type, boolean enabled) {
    public NotificationChannel {
        Objects.requireNonNull(type, "channel type is required");
    }
}
