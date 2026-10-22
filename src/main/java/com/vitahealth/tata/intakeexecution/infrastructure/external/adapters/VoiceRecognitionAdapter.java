package com.vitahealth.tata.intakeexecution.infrastructure.external.adapters;

import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoiceRecognitionPort;
import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.application.result.VoiceRecognitionResult;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.VoiceConfirmationLanguage;
import com.vitahealth.tata.intakeexecution.infrastructure.external.SpeechToTextProviderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates provider observations and contains external failures without exposing provider text.
 */
@Component
public class VoiceRecognitionAdapter implements VoiceRecognitionPort {

  private static final Logger LOGGER = LoggerFactory.getLogger(VoiceRecognitionAdapter.class);

  private final SpeechToTextProviderClient providerClient;

  /** Injects the configured transcription boundary. */
  public VoiceRecognitionAdapter(SpeechToTextProviderClient providerClient) {
    this.providerClient = providerClient;
  }

  /** Maps only valid confidence values and keeps malformed provider output unavailable. */
  @Override
  public VoiceRecognitionResult recognize(
      ValidatedVoiceAudio audio, VoiceConfirmationLanguage language) {
    try {
      var providerResult = providerClient.transcribe(audio, language);
      String transcript = providerResult.transcript();
      double confidence = providerResult.confidence() == null ? 0.0 : providerResult.confidence();

      if (transcript == null || transcript.isBlank()) {
        return VoiceRecognitionResult.unrecognized(null, confidence);
      }

      return VoiceRecognitionResult.recognized(transcript.trim(), confidence);
    } catch (RuntimeException exception) {
      LOGGER.warn("Speech-to-text recognition failed ({})", exception.getClass().getSimpleName());
      return VoiceRecognitionResult.unavailable("speech-to-text provider failure");
    }
  }
}
