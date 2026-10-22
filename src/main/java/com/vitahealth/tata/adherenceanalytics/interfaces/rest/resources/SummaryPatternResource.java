package com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Dominant time-band counts exposed in the summary. */
@Schema(description = "timeBand is MORNING, AFTERNOON, EVENING or NIGHT")
public record SummaryPatternResource(String timeBand, int omittedCount, int lateCount) {}
