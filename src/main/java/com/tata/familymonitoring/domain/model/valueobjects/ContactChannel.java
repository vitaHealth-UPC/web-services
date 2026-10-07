package com.tata.familymonitoring.domain.model.valueobjects;

/** Channel the caregiver can use to reach the older adult when an alert is open. */
public record ContactChannel(ContactChannelType type, String value) {
}
