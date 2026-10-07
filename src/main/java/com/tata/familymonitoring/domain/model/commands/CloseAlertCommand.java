package com.tata.familymonitoring.domain.model.commands;

public record CloseAlertCommand(String olderAdultId, Long alertId) {
}
