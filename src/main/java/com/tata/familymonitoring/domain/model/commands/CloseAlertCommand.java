package com.tata.familymonitoring.domain.model.commands;

public record CloseAlertCommand(Long olderAdultId, Long alertId) {
}
