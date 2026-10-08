package com.tata.familymonitoring.domain.model.commands;

public record MarkAlertAttendedCommand(String olderAdultId, Long alertId) {
}
