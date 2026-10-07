package com.tata.intakeexecution.domain.model.commands;

import java.util.Objects;

public record ConfirmIntakeByVoiceCommand(
        String intakeId,
        byte[] audio,
        String contentType,
        String language
) {
    public ConfirmIntakeByVoiceCommand {
        if (intakeId == null || intakeId.isBlank()) {
            throw new IllegalArgumentException("intakeId is required");
        }
        Objects.requireNonNull(audio, "audio is required");
        if (audio.length == 0) {
            throw new IllegalArgumentException("audio is required");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType is required");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language is required");
        }

        intakeId = intakeId.trim();
        audio = audio.clone();
        contentType = contentType.trim();
        language = language.trim();
    }

    @Override
    public byte[] audio() {
        return audio.clone();
    }
}
