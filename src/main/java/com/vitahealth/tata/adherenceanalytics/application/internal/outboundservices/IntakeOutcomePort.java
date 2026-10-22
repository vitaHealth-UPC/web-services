package com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.IntakeOutcome;
import java.time.Instant;
import java.util.List;

/** Read-only outcome contract; analytics never updates treatment or intake state. */
public interface IntakeOutcomePort {
  /** Reads original outcomes in the requested half-open scheduled interval. */
  List<IntakeOutcome> find(String olderAdultId, Instant from, Instant to);

  /**
   * Reads at most limit ordered owner references after an exclusive cursor for weekly
   * consolidation.
   */
  List<String> olderAdultIdsWithOutcomes(Instant from, Instant to, String after, int limit);
}
