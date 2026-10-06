package com.tata.intakeexecution.application.commands;

import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;

public record ConfirmIntakeCommand(
        String intakeId,
        ConfirmationChannel channel
) {
    public ConfirmIntakeCommand {
        if (intakeId == null || intakeId.isBlank()) {
            throw new IllegalArgumentException("intakeId is required");
        }
        if (channel == null) {
            throw new IllegalArgumentException("confirmation channel is required");
        }

        intakeId = intakeId.trim();
    }
}
