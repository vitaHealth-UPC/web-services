package com.vitahealth.tata.intakeexecution.application.result;

import java.util.Objects;

/** Immutable provider observation with validated confidence; malformed values are never clamped. */
public record VoiceRecognitionResult(
    RecognitionStatus status, String transcript, double confidence, String detail) {
  /** Rejects malformed provider observations before they can reach confirmation validation. */
  public VoiceRecognitionResult {
    Objects.requireNonNull(status, "status");
    if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0)
      throw new IllegalArgumentException(
          "recognition confidence must be finite and between zero and one");
    if (status == RecognitionStatus.RECOGNIZED && (transcript == null || transcript.isBlank()))
      throw new IllegalArgumentException("recognized speech requires a nonblank transcript");
  }

  /** Captures a provider transcript without promoting an invalid confidence to certainty. */
  public static VoiceRecognitionResult recognized(String transcript, double confidence) {
    return new VoiceRecognitionResult(RecognitionStatus.RECOGNIZED, transcript, confidence, null);
  }

  /** Captures valid but unrecognized input without inventing a transcript. */
  public static VoiceRecognitionResult unrecognized(String transcript, double confidence) {
    return new VoiceRecognitionResult(RecognitionStatus.UNRECOGNIZED, transcript, confidence, null);
  }

  /**
   * Represents an unavailable provider using diagnostic text confined to the application boundary.
   */
  public static VoiceRecognitionResult unavailable(String detail) {
    return new VoiceRecognitionResult(RecognitionStatus.UNAVAILABLE, null, 0.0, detail);
  }
}
