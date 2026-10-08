package com.tata.adherenceanalytics.interfaces.acl;
import java.time.Instant;
public interface AdherenceContextFacade {
 record WeeklyMetrics(int confirmedIntakes,int totalIntakes) {}
 WeeklyMetrics weekly(String owner,Instant from,Instant to);
}
