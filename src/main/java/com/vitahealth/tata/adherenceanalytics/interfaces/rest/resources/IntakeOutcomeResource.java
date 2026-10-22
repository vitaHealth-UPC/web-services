package com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources;

import java.time.Instant;

/** Definitive source intake outcome exposed as transport data. */
public record IntakeOutcomeResource(String medicationId, Instant scheduledAt, String status) {}
