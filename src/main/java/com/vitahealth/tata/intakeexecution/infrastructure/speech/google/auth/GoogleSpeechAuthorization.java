package com.vitahealth.tata.intakeexecution.infrastructure.speech.google.auth;

/**
 * Short-lived OAuth request identity and optional ADC quota project, confined to infrastructure.
 */
public record GoogleSpeechAuthorization(String accessToken, String quotaProjectId) {
  /** Requires an explicit credential rather than installing a static or anonymous fallback. */
  public GoogleSpeechAuthorization {
    if (accessToken == null || accessToken.isBlank())
      throw new IllegalArgumentException("Google authorization is required");
  }

  /** Prevents accidental disclosure of bearer credentials through ordinary diagnostic rendering. */
  @Override
  public String toString() {
    return "GoogleSpeechAuthorization[REDACTED]";
  }
}
