package com.tata.intakeexecution.application.commands;

public record MarkIntakeOmittedCommand(String intakeId) {
    public MarkIntakeOmittedCommand {
        if (intakeId == null || intakeId.isBlank()) {
            throw new IllegalArgumentException("intakeId is required");
        }
        intakeId = intakeId.trim();
    }
}