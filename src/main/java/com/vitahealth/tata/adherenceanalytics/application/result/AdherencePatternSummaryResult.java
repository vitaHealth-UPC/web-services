package com.vitahealth.tata.adherenceanalytics.application.result;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.TimeBand;

/** Issue counts for the dominant calendar time band. */
public record AdherencePatternSummaryResult(TimeBand timeBand, int omittedCount, int lateCount) {}
