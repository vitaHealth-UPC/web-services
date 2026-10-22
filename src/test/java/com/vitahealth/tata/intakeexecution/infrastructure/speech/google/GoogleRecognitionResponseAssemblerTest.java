package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.intakeexecution.domain.services.VoiceConfirmationValidationService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies ranking, segment ordering and conservative provider confidence without clinical
 * inference.
 */
class GoogleRecognitionResponseAssemblerTest {
  @Test
  void principalNegationIsNeverReplacedWithAnAffirmativeAlternative() {
    var response =
        new GoogleRecognitionResponse(
            List.of(
                new GoogleSpeechSegment(
                    List.of(
                        new GoogleSpeechAlternative("No tomé Losartán", 0.99),
                        new GoogleSpeechAlternative("Ya tomé Losartán", 1.0)))));
    var result = GoogleRecognitionResponseAssembler.toResponse(response);
    assertEquals("No tomé Losartán", result.transcript());
    assertEquals(
        false,
        new VoiceConfirmationValidationService()
            .isValidConfirmation(result.transcript(), result.confidence(), "Losartán"));
  }

  @Test
  void joinsSequentialSegmentsAndUsesTheLeastValidConfidence() {
    var result =
        GoogleRecognitionResponseAssembler.toResponse(
            new GoogleRecognitionResponse(
                List.of(segment("Ya tomé", 0.95), segment("Losartán", 0.81))));
    assertEquals("Ya tomé Losartán", result.transcript());
    assertEquals(0.81, result.confidence());
  }

  @Test
  void absentConfidenceDoesNotBecomeCertainSpeech() {
    var result =
        GoogleRecognitionResponseAssembler.toResponse(
            new GoogleRecognitionResponse(
                List.of(segment("Ya tomé", 0.99), segment("Losartán", null))));
    assertEquals(0.0, result.confidence());
    assertEquals(
        false,
        new VoiceConfirmationValidationService()
            .isValidConfirmation(result.transcript(), result.confidence(), "Losartán"));
  }

  @Test
  void rejectsMalformedConfidenceRatherThanClampingIt() {
    for (var value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -0.1, 1.01})
      assertThrows(
          IllegalStateException.class,
          () ->
              GoogleRecognitionResponseAssembler.toResponse(
                  new GoogleRecognitionResponse(List.of(segment("Ya tomé Losartán", value)))));
  }

  @Test
  void emptyResponsesAndSegmentsDoNotInventASpeechResult() {
    for (var response :
        List.of(
            new GoogleRecognitionResponse(null),
            new GoogleRecognitionResponse(
                List.of(new GoogleSpeechSegment(null), segment(" ", 0.9))))) {
      var result = GoogleRecognitionResponseAssembler.toResponse(response);
      assertNull(result.transcript());
      assertEquals(0.0, result.confidence());
    }
  }

  private static GoogleSpeechSegment segment(String text, Double confidence) {
    return new GoogleSpeechSegment(List.of(new GoogleSpeechAlternative(text, confidence)));
  }
}
