package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

/** Provider request envelope; its DTOs remain outside application and domain contracts. */
public record GoogleRecognitionRequest(
    GoogleRecognitionConfig config, GoogleRecognitionAudio audio) {
  /** Renders configuration without recording contents. */
  @Override
  public String toString() {
    return "GoogleRecognitionRequest[config=" + config + ", audio=REDACTED]";
  }
}
