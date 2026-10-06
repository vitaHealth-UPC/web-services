package com.tata.familymonitoring.domain.model.commands;

public record MarkAlertAttendedCommand(Long olderAdultId, Long alertId) {
}
