package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Sequential provider segment whose first hypothesis is the most probable alternative. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleSpeechSegment(List<GoogleSpeechAlternative> alternatives) {
  /** Snapshots ranked hypotheses and permits an empty segment without inventing recognition. */
  public GoogleSpeechSegment {
    alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
  }
}
