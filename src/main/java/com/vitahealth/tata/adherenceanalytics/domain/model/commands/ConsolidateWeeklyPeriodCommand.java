package com.vitahealth.tata.adherenceanalytics.domain.model.commands;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherencePeriod;
import java.time.Instant;
import java.util.Objects;

/** Requests a single immutable consolidation for a validated owner-period-zone identity. */
public record ConsolidateWeeklyPeriodCommand(AdherencePeriod period) {
  /** Requires the complete domain period before source reads or persistence begin. */
  public ConsolidateWeeklyPeriodCommand {
    Objects.requireNonNull(period, "adherence period");
  }

  /** Maps explicit boundary inputs through the shared domain period invariants. */
  public ConsolidateWeeklyPeriodCommand(String owner, Instant from, Instant to, String zone) {
    this(new AdherencePeriod(owner, from, to, zone));
  }
}
