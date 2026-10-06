package com.tata.familymonitoring.domain.ports;

import com.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;

/** Port to Adherence Analytics. */
public interface IAdherenceSummaryPort {

  AdherenceSnapshot getWeeklySummary(String olderAdultId);
}
