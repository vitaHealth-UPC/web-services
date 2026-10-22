package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

/** Base64 content required by the Google JSON protocol; not a persisted audio object. */
public record GoogleRecognitionAudio(String content) {
  /** Prevents raw recordings appearing in ordinary DTO diagnostics. */
  @Override
  public String toString() {
    return "GoogleRecognitionAudio[REDACTED]";
  }
}
