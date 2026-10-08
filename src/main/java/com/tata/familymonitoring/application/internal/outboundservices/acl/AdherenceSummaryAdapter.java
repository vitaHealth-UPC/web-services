package com.tata.familymonitoring.application.internal.outboundservices.acl;

import com.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;
import com.tata.familymonitoring.domain.ports.IAdherenceSummaryPort;
import com.tata.adherenceanalytics.interfaces.acl.AdherenceContextFacade;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Reads the rolling seven-day adherence indicators through the Analytics application contract.
 */
@Component
public class AdherenceSummaryAdapter implements IAdherenceSummaryPort {
  private final AdherenceContextFacade queries;
  public AdherenceSummaryAdapter(AdherenceContextFacade queries) { this.queries = queries; }

  @Override
  public AdherenceSnapshot getWeeklySummary(String olderAdultId) {
    var to = Instant.now();
    var metrics = queries.weekly(olderAdultId, to.minus(Duration.ofDays(7)), to);
    return new AdherenceSnapshot(metrics.confirmedIntakes(), metrics.totalIntakes());
  }
}
