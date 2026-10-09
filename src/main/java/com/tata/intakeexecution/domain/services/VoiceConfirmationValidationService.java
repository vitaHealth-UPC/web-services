package com.tata.intakeexecution.domain.services;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

/** Validates an explicit completed intake; recognition confidence alone is insufficient. */
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
        if (normalizedTranscript.matches(".*\\b(no|nunca|tampoco|todavia|quizas|quiza|not|never|maybe|think)\\b.*")) {
            return false;
        }

        if (containsWords(normalizedTranscript, "don t") || containsWords(normalizedTranscript, "didn t") || containsWords(normalizedTranscript, "haven t")) {
            return false;
        }

        boolean hasConfirmationIntent =
                (containsWords(normalizedTranscript, "confirmo") && containsWords(normalizedTranscript, "tome"))
                        || containsWords(normalizedTranscript, "ya tome")
                        || containsWords(normalizedTranscript, "he tomado")
                        || containsWords(normalizedTranscript, "me tome")
                        || containsWords(normalizedTranscript, "tome mi")
                        || containsWords(normalizedTranscript, "i took")
                        || containsWords(normalizedTranscript, "i have taken");

        if (!hasConfirmationIntent) {
            return false;
        }

        boolean referencesMedicationGenerically =
                containsWords(normalizedTranscript, "medicamento")
                        || containsWords(normalizedTranscript, "pastilla")
                        || containsWords(normalizedTranscript, "medication")
                        || containsWords(normalizedTranscript, "pill");

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
