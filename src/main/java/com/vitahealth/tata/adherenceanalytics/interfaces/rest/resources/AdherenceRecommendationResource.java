package com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources;

/** Follow-up recommendation supported by recorded omission evidence. */
public record AdherenceRecommendationResource(String medicationId, String code, int evidenceDays) {}
