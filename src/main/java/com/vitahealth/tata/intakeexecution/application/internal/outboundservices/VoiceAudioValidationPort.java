package com.vitahealth.tata.intakeexecution.application.internal.outboundservices;

import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;

/** Inspects recording bytes independently of speech recognition and clinical intent. */
@FunctionalInterface
public interface VoiceAudioValidationPort {
  /**
   * Rejects unsupported or malformed input with IllegalArgumentException before external delivery.
   */
  ValidatedVoiceAudio validate(byte[] audio, String contentType);
}
