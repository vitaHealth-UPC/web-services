package com.vitahealth.tata.adherenceanalytics.application.internal.acl;

import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherencePeriodQuery;
import com.vitahealth.tata.adherenceanalytics.interfaces.acl.AdherenceContextFacade;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** Adapts the public cross-context facade to the owning application query boundary. */
@Service
public class AdherenceContextFacadeImpl implements AdherenceContextFacade {
  private final AdherenceQueryService queries;

  /** Injects the current-evidence query service. */
  public AdherenceContextFacadeImpl(AdherenceQueryService queries) {
    this.queries = queries;
  }

  /** Preserves definitive-only counts without fabricating empty evidence. */
  @Override
  public WeeklyMetrics weekly(String owner, Instant from, Instant to) {
    var metrics = queries.weekly(new GetAdherencePeriodQuery(owner, from, to));
    return new WeeklyMetrics(metrics.confirmedIntakes(), metrics.totalIntakes());
  }
}
