package com.vitahealth.tata.adherenceanalytics.domain.model.queries;

import java.time.Instant;
import java.time.ZoneId;

/** Validated owner, period and calendar reference shared by adherence view queries. */
public record GetAdherenceViewQuery(String olderAdultId, int periodDays, ZoneId zone, Instant now) {
  public static final int MAX_PERIOD_DAYS = 31;

  /** Normalizes the owner and rejects incomplete or unsupported query parameters. */
  public GetAdherenceViewQuery {
    if (olderAdultId == null || olderAdultId.isBlank()) {
      throw new IllegalArgumentException("older adult is required");
    }
    olderAdultId = olderAdultId.trim();
    if (periodDays < 1 || periodDays > MAX_PERIOD_DAYS) {
      throw new IllegalArgumentException(
          "period must be between one and " + MAX_PERIOD_DAYS + " days");
    }
    if (zone == null || now == null) {
      throw new IllegalArgumentException("calendar zone and reference instant are required");
    }
  }
}
