package com.tata.intakeexecution.interfaces.rest.resources;

import com.tata.intakeexecution.application.models.VoiceConfirmationResult.VoiceConfirmationStatus;

public record VoiceConfirmationResource(
        VoiceConfirmationStatus status,
        String transcript,
        double confidence,
        IntakeResource intake
) {
}
