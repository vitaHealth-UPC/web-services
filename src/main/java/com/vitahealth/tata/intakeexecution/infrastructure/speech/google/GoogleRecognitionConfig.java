package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

/** Exact Google v1 recognition configuration derived from inspected WAV metadata. */
public record GoogleRecognitionConfig(
    String encoding, int sampleRateHertz, String languageCode, int maxAlternatives) {}
