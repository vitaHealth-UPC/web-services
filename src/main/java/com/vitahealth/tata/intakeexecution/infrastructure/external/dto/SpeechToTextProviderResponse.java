package com.vitahealth.tata.intakeexecution.infrastructure.external.dto;

/** Provider-neutral transcript observation; confidence may be absent without implying certainty. */
public record SpeechToTextProviderResponse(String transcript, Double confidence) {}
