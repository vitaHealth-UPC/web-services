package com.tata.accessibilitypreferences.domain.model.valueobjects;

import java.time.LocalTime;

/**
 * Daily interval in which non-critical notifications are held back. The range may cross midnight,
 * for example 22:00 to 07:00.
 */
public record QuietHoursRange(int startHour, int startMinute, int endHour, int endMinute) {

    public QuietHoursRange {
        requireInRange(startHour, 23, "startHour");
        requireInRange(startMinute, 59, "startMinute");
        requireInRange(endHour, 23, "endHour");
        requireInRange(endMinute, 59, "endMinute");
        if (startHour == endHour && startMinute == endMinute) {
            throw new IllegalArgumentException("quiet hours must start and end at different times");
        }
    }

    public static QuietHoursRange between(LocalTime start, LocalTime end) {
        return new QuietHoursRange(start.getHour(), start.getMinute(), end.getHour(), end.getMinute());
    }

    public LocalTime start() {
        return LocalTime.of(startHour, startMinute);
    }

    public LocalTime end() {
        return LocalTime.of(endHour, endMinute);
    }

    /** True when the given time falls inside the range. The end time itself is outside. */
    public boolean contains(int hour, int minute) {
        int moment = hour * 60 + minute;
        int from = startHour * 60 + startMinute;
        int to = endHour * 60 + endMinute;
        if (from < to) {
            return moment >= from && moment < to;
        }
        return moment >= from || moment < to;
    }

    private static void requireInRange(int value, int max, String field) {
        if (value < 0 || value > max) {
            throw new IllegalArgumentException(field + " must be between 0 and " + max);
        }
    }
}
