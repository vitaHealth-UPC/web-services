package com.vitahealth.tata.intakeexecution.interfaces.rest.resources;

import com.vitahealth.tata.intakeexecution.application.result.VoiceConfirmationResult.VoiceConfirmationStatus;

/** Public recognition outcome with optional evidence of a definitive intake confirmation. */
public record VoiceConfirmationResource(
    VoiceConfirmationStatus status, String transcript, double confidence, IntakeResource intake) {}
