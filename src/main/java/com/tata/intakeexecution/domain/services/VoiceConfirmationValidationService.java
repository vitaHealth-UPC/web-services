package com.tata.intakeexecution.domain.services;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

public final class VoiceConfirmationValidationService {

    public static final double MINIMUM_CONFIDENCE = 0.70;

    public boolean isValidConfirmation(
            String transcript,
            double confidence,
            String medicationName
    ) {
        if (transcript == null
                || transcript.isBlank()
                || confidence < MINIMUM_CONFIDENCE) {
            return false;
        }

        String normalizedTranscript = normalize(transcript);
        String normalizedMedication = normalize(medicationName);

        boolean hasConfirmationIntent =
                normalizedTranscript.contains("confirmo")
                        || normalizedTranscript.contains("ya tome")
                        || normalizedTranscript.contains("he tomado")
                        || normalizedTranscript.contains("me tome")
                        || normalizedTranscript.contains("tome mi");

        if (!hasConfirmationIntent) {
            return false;
        }

        boolean referencesMedicationGenerically =
                normalizedTranscript.contains("medicamento")
                        || normalizedTranscript.contains("pastilla");

        boolean referencesExpectedMedication = Arrays.stream(normalizedMedication.split("\\s+"))
                .filter(token -> token.length() >= 4)
                .findFirst()
                .map(normalizedTranscript::contains)
                .orElse(false);

        return referencesMedicationGenerically || referencesExpectedMedication;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
