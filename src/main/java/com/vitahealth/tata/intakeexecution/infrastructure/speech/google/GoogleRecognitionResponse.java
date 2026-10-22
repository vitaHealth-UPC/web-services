package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Google v1 response with sequential segments, independent of the public confirmation outcome. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleRecognitionResponse(List<GoogleSpeechSegment> results) {
  /** Snapshots segments; an absent results field represents no recognized speech. */
  public GoogleRecognitionResponse {
    results = results == null ? List.of() : List.copyOf(results);
  }
}
