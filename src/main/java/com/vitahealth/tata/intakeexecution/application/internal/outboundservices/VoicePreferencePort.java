package com.vitahealth.tata.intakeexecution.application.internal.outboundservices;

/** Port to Accessibility & Preferences: whether the user allows confirming intakes by voice. */
@FunctionalInterface
public interface VoicePreferencePort {
  /** Reads the current preference; technical failures must propagate rather than allow voice. */
  boolean isVoiceConfirmationEnabled(String olderAdultId);
}
