package com.vitahealth.tata.intakeexecution.infrastructure.external;

import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.VoiceConfirmationLanguage;
import com.vitahealth.tata.intakeexecution.infrastructure.external.dto.SpeechToTextProviderResponse;

/** Technical transcription capability implemented by the configured concrete provider. */
@FunctionalInterface
public interface SpeechToTextProviderClient {
  /** Transcribes already inspected audio without prescribing any clinical intake transition. */
  SpeechToTextProviderResponse transcribe(
      ValidatedVoiceAudio audio, VoiceConfirmationLanguage language);
}
