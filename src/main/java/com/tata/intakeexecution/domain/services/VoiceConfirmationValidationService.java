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
                || !Double.isFinite(confidence)
                || confidence > 1.0
                || confidence < MINIMUM_CONFIDENCE) {
            return false;
        }

        String normalizedTranscript = normalize(transcript);
        String normalizedMedication = normalize(medicationName);

        // A negated or uncertain utterance must never record an intake.
        if (normalizedTranscript.matches(".*\\b(no|nunca|tampoco|todavia|quizas|quiza)\\b.*")) {
            return false;
        }

        boolean hasConfirmationIntent =
                containsWords(normalizedTranscript, "confirmo")
                        || containsWords(normalizedTranscript, "ya tome")
                        || containsWords(normalizedTranscript, "he tomado")
                        || containsWords(normalizedTranscript, "me tome")
                        || containsWords(normalizedTranscript, "tome mi");

        if (!hasConfirmationIntent) {
            return false;
        }

        boolean referencesMedicationGenerically =
                containsWords(normalizedTranscript, "medicamento")
                        || containsWords(normalizedTranscript, "pastilla");

        boolean referencesExpectedMedication = Arrays.stream(normalizedMedication.split("\\s+"))
                .filter(token -> token.length() >= 4)
                .findFirst()
                .map(token -> containsWords(normalizedTranscript, token))
                .orElse(false);

        return referencesMedicationGenerically || referencesExpectedMedication;
    }

    private boolean containsWords(String transcript, String words) {
        return (" " + transcript + " ").contains(" " + words + " ");
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
