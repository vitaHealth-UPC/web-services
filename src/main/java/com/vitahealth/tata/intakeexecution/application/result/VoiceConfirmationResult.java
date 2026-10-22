package com.vitahealth.tata.intakeexecution.application.result;

/** Voice confirmation outcome with optional confirmed intake evidence. */
public record VoiceConfirmationResult(
    VoiceConfirmationStatus status, String transcript, double confidence, IntakeResult intake) {
  /** Outcome of recognition, validation and the final confirmation transition. */
  public enum VoiceConfirmationStatus {
    CONFIRMED,
    ALREADY_CONFIRMED,
    NOT_RECOGNIZED,
    NOT_VALIDATED,
    PROVIDER_UNAVAILABLE
  }
}
