package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One ranked provider hypothesis; absence of confidence remains explicit. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleSpeechAlternative(String transcript, Double confidence) {
  /** Keeps clinical utterances out of ordinary DTO diagnostics. */
  @Override
  public String toString() {
    return "GoogleSpeechAlternative[transcript=REDACTED]";
  }
}
