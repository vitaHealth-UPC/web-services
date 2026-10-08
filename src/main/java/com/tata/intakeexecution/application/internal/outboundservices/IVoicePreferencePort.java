package com.tata.intakeexecution.application.internal.outboundservices;

/** Port to Accessibility & Preferences: whether the user allows confirming intakes by voice. */
@FunctionalInterface
public interface IVoicePreferencePort {
    boolean isVoiceConfirmationEnabled(String olderAdultId);
}
