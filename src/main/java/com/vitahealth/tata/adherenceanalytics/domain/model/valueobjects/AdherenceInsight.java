package com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects;

/** Follow-up recommendation code and its supporting medication evidence. */
public record AdherenceInsight(String medicationId, String code, int evidenceDays) {}
