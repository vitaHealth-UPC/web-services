package com.vitahealth.tata.adherenceanalytics.application.result;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.TimeBand;

/** Detected time-band concentration of late and omitted intakes. */
public record AdherenceInsightPatternResult(
    String type, TimeBand timeBand, int omittedCount, int lateCount, int fromHour, int toHour) {}
