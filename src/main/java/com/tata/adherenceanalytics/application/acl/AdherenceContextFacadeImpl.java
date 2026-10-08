package com.tata.adherenceanalytics.application.acl;
import com.tata.adherenceanalytics.interfaces.acl.AdherenceContextFacade;
import com.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import java.time.Instant;
import org.springframework.stereotype.Service;
@Service
public class AdherenceContextFacadeImpl implements AdherenceContextFacade {
 private final AdherenceQueryService queries;
 public AdherenceContextFacadeImpl(AdherenceQueryService queries) { this.queries=queries; }
 @Override public WeeklyMetrics weekly(String owner,Instant from,Instant to) {
  var metrics=queries.weekly(owner,from,to);return new WeeklyMetrics(metrics.confirmedIntakes(),metrics.totalIntakes());
 }
}
