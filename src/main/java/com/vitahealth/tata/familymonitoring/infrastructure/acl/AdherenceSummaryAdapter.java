package com.vitahealth.tata.familymonitoring.infrastructure.acl;

import com.vitahealth.tata.adherenceanalytics.interfaces.acl.AdherenceContextFacade;
import com.vitahealth.tata.familymonitoring.application.exceptions.InvalidMonitoringSourceException;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;
import com.vitahealth.tata.familymonitoring.domain.ports.AdherenceSummaryPort;
import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Reads the rolling seven-day adherence indicators through the Analytics application contract. */
@Component
public class AdherenceSummaryAdapter implements AdherenceSummaryPort {
  private final AdherenceContextFacade queries;
  private final Clock clock;

  /** Injects analytics queries and the application clock for the rolling week. */
  public AdherenceSummaryAdapter(AdherenceContextFacade queries, Clock clock) {
    this.queries = queries;
    this.clock = clock;
  }

  /** Maps source metrics for the seven elapsed days ending at the application clock. */
  @Override
  public AdherenceSnapshot getWeeklySummary(String olderAdultId) {
    var to = clock.instant();
    try {
      var metrics = queries.weekly(olderAdultId, to.minus(Duration.ofDays(7)), to);
      return new AdherenceSnapshot(metrics.confirmedIntakes(), metrics.totalIntakes());
    } catch (IllegalArgumentException
        | IllegalStateException
        | NullPointerException invalidSource) {
      throw new InvalidMonitoringSourceException(invalidSource);
    }
  }
}
