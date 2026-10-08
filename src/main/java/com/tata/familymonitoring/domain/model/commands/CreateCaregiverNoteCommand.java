package com.tata.familymonitoring.domain.model.commands;

public record CreateCaregiverNoteCommand(String olderAdultId, String familiarId, String text) {
}
