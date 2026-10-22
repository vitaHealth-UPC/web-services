package com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Local hour interval and counts for a detected issue pattern. */
@Schema(
    description =
        "type is OMISSION or LATENESS; hours are local, from inclusive to exclusive; an end at or"
            + " before the start is on the next day")
public record InsightPatternResource(
    String type, String timeBand, int omittedCount, int lateCount, int fromHour, int toHour) {}
