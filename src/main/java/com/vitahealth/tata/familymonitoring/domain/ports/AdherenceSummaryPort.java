package com.vitahealth.tata.familymonitoring.domain.ports;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;

/** Reads source adherence counts after the caller has authorized caregiver access. */
public interface AdherenceSummaryPort {

  /**
   * Returns counts for seven elapsed days ending at the application clock; zero counts are valid.
   */
  AdherenceSnapshot getWeeklySummary(String olderAdultId);
}
