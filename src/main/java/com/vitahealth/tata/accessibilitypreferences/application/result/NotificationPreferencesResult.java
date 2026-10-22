package com.vitahealth.tata.accessibilitypreferences.application.result;

import java.time.LocalTime;

/** What other Bounded Contexts need to know before sending a notification. */
public record NotificationPreferencesResult(
    boolean pushEnabled,
    boolean smsEnabled,
    boolean emailEnabled,
    LocalTime quietHoursStart,
    LocalTime quietHoursEnd) {}
