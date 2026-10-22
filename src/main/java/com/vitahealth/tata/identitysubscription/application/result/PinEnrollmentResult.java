package com.vitahealth.tata.identitysubscription.application.result;

/** Evidence of PIN enrollment for an adult, without the raw credential or stored hash. */
public record PinEnrollmentResult(String olderAdultId) {}
