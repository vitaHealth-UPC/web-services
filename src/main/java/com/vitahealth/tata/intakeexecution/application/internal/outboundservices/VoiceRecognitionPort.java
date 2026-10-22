package com.vitahealth.tata.intakeexecution.application.internal.outboundservices;

import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.application.result.VoiceRecognitionResult;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.VoiceConfirmationLanguage;

/** Recognition of inspected audio, independent of provider DTOs and clinical confirmation rules. */
@FunctionalInterface
public interface VoiceRecognitionPort {
  /**
   * Returns a validated observation; recognition alone does not authorize a clinical transition.
   */
  VoiceRecognitionResult recognize(ValidatedVoiceAudio audio, VoiceConfirmationLanguage language);
}
