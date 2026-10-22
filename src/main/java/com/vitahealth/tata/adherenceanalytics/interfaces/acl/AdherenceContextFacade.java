package com.vitahealth.tata.adherenceanalytics.interfaces.acl;

import java.time.Instant;

/** Minimal adherence evidence shared with other contexts without exposing persistence. */
public interface AdherenceContextFacade {
  /** Confirmed (on-time plus late) and definitive (confirmed plus omitted) intake counts. */
  record WeeklyMetrics(int confirmedIntakes, int totalIntakes) {
    /** Rejects impossible evidence instead of exposing an invalid adherence denominator. */
    public WeeklyMetrics {
      if (confirmedIntakes < 0 || totalIntakes < 0 || confirmedIntakes > totalIntakes) {
        throw new IllegalArgumentException("weekly adherence counts are inconsistent");
      }
    }
  }

  /** Reads an explicit half-open period; this boundary does not grant caller authorization. */
  WeeklyMetrics weekly(String owner, Instant from, Instant to);
}
