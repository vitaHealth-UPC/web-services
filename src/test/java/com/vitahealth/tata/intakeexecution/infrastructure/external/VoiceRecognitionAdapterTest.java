package com.vitahealth.tata.intakeexecution.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.intakeexecution.application.result.RecognitionStatus;
import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.VoiceConfirmationLanguage;
import com.vitahealth.tata.intakeexecution.infrastructure.audio.WavVoiceAudioValidator;
import com.vitahealth.tata.intakeexecution.infrastructure.external.adapters.VoiceRecognitionAdapter;
import com.vitahealth.tata.intakeexecution.infrastructure.external.dto.SpeechToTextProviderResponse;
import com.vitahealth.tata.intakeexecution.support.WavAudioFixture;
import org.junit.jupiter.api.Test;

class VoiceRecognitionAdapterTest {

  @Test
  void mapsProviderTranscriptAndConfidence() {
    var adapter =
        new VoiceRecognitionAdapter(
            (audio, language) ->
                new SpeechToTextProviderResponse("Confirmo que tomé Losartán.", 0.93));

    var result = adapter.recognize(recording(), new VoiceConfirmationLanguage("es-419"));

    assertThat(result.status()).isEqualTo(RecognitionStatus.RECOGNIZED);
    assertThat(result.transcript()).isEqualTo("Confirmo que tomé Losartán.");
    assertThat(result.confidence()).isEqualTo(0.93);
  }

  @Test
  void blankTranscriptIsUnrecognized() {
    var adapter =
        new VoiceRecognitionAdapter(
            (audio, language) -> new SpeechToTextProviderResponse(" ", 0.31));

    var result = adapter.recognize(recording(), new VoiceConfirmationLanguage("es-419"));

    assertThat(result.status()).isEqualTo(RecognitionStatus.UNRECOGNIZED);
    assertThat(result.confidence()).isEqualTo(0.31);
  }

  @Test
  void providerFailureBecomesControlledUnavailableOutcome() {
    var adapter =
        new VoiceRecognitionAdapter(
            (audio, language) -> {
              throw new IllegalStateException("provider timeout");
            });

    var result = adapter.recognize(recording(), new VoiceConfirmationLanguage("es-419"));

    assertThat(result.status()).isEqualTo(RecognitionStatus.UNAVAILABLE);
    assertThat(result.detail()).isEqualTo("speech-to-text provider failure");
  }

  @Test
  void malformedConfidenceNeverBecomesCertainRecognizedSpeech() {
    for (var confidence :
        new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.1, 1.01}) {
      var adapter =
          new VoiceRecognitionAdapter(
              (audio, language) ->
                  new SpeechToTextProviderResponse("Confirmo que tomé Losartán", confidence));
      var result = adapter.recognize(recording(), new VoiceConfirmationLanguage("es-419"));
      assertThat(result.status()).isEqualTo(RecognitionStatus.UNAVAILABLE);
      assertThat(result.confidence()).isZero();
      assertThat(result.transcript()).isNull();
    }
  }

  private static ValidatedVoiceAudio recording() {
    return new WavVoiceAudioValidator().validate(WavAudioFixture.shortRecording(), "audio/wav");
  }
}
