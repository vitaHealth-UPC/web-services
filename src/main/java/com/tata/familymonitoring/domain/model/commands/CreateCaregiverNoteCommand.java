package com.tata.familymonitoring.domain.model.commands;

public record CreateCaregiverNoteCommand(Long olderAdultId, Long familiarId, String text) {
}
