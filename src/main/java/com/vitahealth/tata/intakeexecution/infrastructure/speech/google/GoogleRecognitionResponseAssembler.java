package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

import com.vitahealth.tata.intakeexecution.infrastructure.external.dto.SpeechToTextProviderResponse;
import java.util.ArrayList;
import java.util.Objects;

/** Preserves top-ranked segment order and the least confidence of all recognized segments. */
public final class GoogleRecognitionResponseAssembler {
  private GoogleRecognitionResponseAssembler() {}

  /** Never substitutes a lower-ranked affirmation for the provider's principal hypothesis. */
  public static SpeechToTextProviderResponse toResponse(GoogleRecognitionResponse response) {
    Objects.requireNonNull(response, "Google recognition response");
    var transcript = new ArrayList<String>();
    double confidence = 1.0;
    for (var segment : response.results()) {
      if (segment.alternatives().isEmpty()) continue;
      var alternative = segment.alternatives().getFirst();
      var value = alternative.confidence();
      if (value != null && (!Double.isFinite(value) || value < 0.0 || value > 1.0))
        throw new IllegalStateException("Google recognition returned invalid confidence");
      if (alternative.transcript() == null || alternative.transcript().isBlank()) continue;
      transcript.add(alternative.transcript().trim());
      confidence = Math.min(confidence, value == null ? 0.0 : value);
    }
    return transcript.isEmpty()
        ? new SpeechToTextProviderResponse(null, 0.0)
        : new SpeechToTextProviderResponse(String.join(" ", transcript), confidence);
  }
}
