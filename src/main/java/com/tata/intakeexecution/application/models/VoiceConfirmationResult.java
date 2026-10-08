package com.tata.intakeexecution.application.models;

public record VoiceConfirmationResult(
        VoiceConfirmationStatus status,
        String transcript,
        double confidence,
        IntakeResult intake
) {
    public enum VoiceConfirmationStatus {
        CONFIRMED,
        ALREADY_CONFIRMED,
        NOT_RECOGNIZED,
        NOT_VALIDATED,
        PROVIDER_UNAVAILABLE
    }
}
