package com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources;

import java.time.LocalDate;

/** Calendar-day evidence of repeated omissions for one medication. */
public record AdherencePatternResource(
    String medicationId, int omissionDays, LocalDate firstDay, LocalDate lastDay) {}
