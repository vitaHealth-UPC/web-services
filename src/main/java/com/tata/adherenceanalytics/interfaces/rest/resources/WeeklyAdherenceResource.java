package com.tata.adherenceanalytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Weekly adherence calculated from persisted intake outcomes")
public record WeeklyAdherenceResource(String olderAdultId, Instant from, Instant to, int confirmedIntakes,
        int totalIntakes, double percentage, int onTimeIntakes, int lateIntakes, int omittedIntakes) {}
