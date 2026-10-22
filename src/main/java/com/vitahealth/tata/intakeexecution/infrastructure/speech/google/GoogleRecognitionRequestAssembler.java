package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.VoiceConfirmationLanguage;
import java.util.Base64;

/** Maps validated PCM input and explicit locale into the Google v1 JSON representation. */
public final class GoogleRecognitionRequestAssembler {
  private GoogleRecognitionRequestAssembler() {}

  /**
   * Maps the product's Latin-American locale explicitly to the supported Peruvian Spanish model.
   */
  public static GoogleRecognitionRequest toRequest(
      ValidatedVoiceAudio audio, VoiceConfirmationLanguage language) {
    var providerLanguage = language.code().equals("es-419") ? "es-PE" : language.code();
    return new GoogleRecognitionRequest(
        new GoogleRecognitionConfig("LINEAR16", audio.sampleRateHertz(), providerLanguage, 1),
        new GoogleRecognitionAudio(Base64.getEncoder().encodeToString(audio.audio())));
  }
}
