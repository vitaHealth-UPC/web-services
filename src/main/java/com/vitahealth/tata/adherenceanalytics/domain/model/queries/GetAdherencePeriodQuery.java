package com.vitahealth.tata.adherenceanalytics.domain.model.queries;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherencePeriod;
import java.time.Instant;
import java.util.Objects;

/** Reads explicit-period evidence using one validated owner and calendar identity. */
public record GetAdherencePeriodQuery(AdherencePeriod period) {
  /** Requires validated bounds before source evidence is read. */
  public GetAdherencePeriodQuery {
    Objects.requireNonNull(period, "adherence period");
  }

  /** Validates the calendar zone used by pattern analysis. */
  public GetAdherencePeriodQuery(String owner, Instant from, Instant to, String zone) {
    this(new AdherencePeriod(owner, from, to, zone));
  }

  /** Uses UTC for count/history operations whose explicit instant bounds are zone-independent. */
  public GetAdherencePeriodQuery(String owner, Instant from, Instant to) {
    this(owner, from, to, "UTC");
  }
}
