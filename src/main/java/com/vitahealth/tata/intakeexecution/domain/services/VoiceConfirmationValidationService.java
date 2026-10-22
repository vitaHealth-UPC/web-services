package com.vitahealth.tata.intakeexecution.domain.services;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/** Validates an explicit completed intake; recognition confidence alone is insufficient. */
public final class VoiceConfirmationValidationService {

  /** Minimum recognition confidence; an explicit completed action is also required. */
  public static final double MINIMUM_CONFIDENCE = 0.70;

  private static final Pattern NEGATION_OR_UNCERTAINTY =
      Pattern.compile(
          "\\b(no|nunca|tampoco|todavia|quizas|quiza|creo|supongo|not|never|maybe|think)\\b");

  /**
   * Accepts an explicit completed consumption for the selected intake's medication. Generic
   * medication references apply only to that already selected intake. Questions, explicit
   * uncertainty and negation fail even at high recognition confidence.
   */
  public boolean isValidConfirmation(String transcript, double confidence, String medicationName) {
    if (transcript == null
        || transcript.isBlank()
        || transcript.indexOf('?') >= 0
        || transcript.indexOf('¿') >= 0
        || !Double.isFinite(confidence)
        || confidence > 1.0
        || confidence < MINIMUM_CONFIDENCE) {
      return false;
    }

    String normalizedTranscript = normalize(transcript);
    return !hasNegationOrUncertainty(normalizedTranscript)
        && hasCompletedConsumption(normalizedTranscript)
        && referencesMedication(normalizedTranscript, normalize(medicationName));
  }

  private boolean hasNegationOrUncertainty(String transcript) {
    return NEGATION_OR_UNCERTAINTY.matcher(transcript).find()
        || containsWords(transcript, "tal vez")
        || containsWords(transcript, "a lo mejor")
        || containsWords(transcript, "don t")
        || containsWords(transcript, "didn t")
        || containsWords(transcript, "haven t");
  }

  private boolean hasCompletedConsumption(String transcript) {
    return (containsWords(transcript, "confirmo") && containsWords(transcript, "tome"))
        || containsWords(transcript, "ya tome")
        || containsWords(transcript, "he tomado")
        || containsWords(transcript, "me tome")
        || containsWords(transcript, "tome mi")
        || containsWords(transcript, "i took")
        || containsWords(transcript, "i have taken");
  }

  private boolean referencesMedication(String transcript, String medicationName) {
    boolean referencesMedicationGenerically =
        containsWords(transcript, "medicamento")
            || containsWords(transcript, "pastilla")
            || containsWords(transcript, "medication")
            || containsWords(transcript, "pill");

    boolean referencesExpectedMedication =
        Arrays.stream(medicationName.split("\\s+"))
            .filter(token -> token.length() >= 4)
            .findFirst()
            .map(token -> containsWords(transcript, token))
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
    String withoutAccents =
        Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return withoutAccents
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9\\s]", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }
}
