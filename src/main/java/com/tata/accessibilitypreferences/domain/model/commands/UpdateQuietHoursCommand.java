package com.tata.accessibilitypreferences.domain.model.commands;

import com.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;

/** A null range removes the quiet hours. */
public record UpdateQuietHoursCommand(String userId, QuietHoursRange quietHours) {}
