package com.tata.intakeexecution.infrastructure.external.dto;

public record SpeechToTextProviderResponse(
        String transcript,
        Double confidence
) {
}
