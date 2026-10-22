package com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;

/** Immutable captured adherence evidence exposed by consolidation endpoints. */
public record AdherenceSnapshotResource(
    String id,
    String olderAdultId,
    Instant from,
    Instant to,
    String zone,
    Instant consolidatedAt,
    int confirmedIntakes,
    int totalIntakes,
    double percentage,
    int onTimeIntakes,
    int lateIntakes,
    int omittedIntakes,
    List<AdherencePatternResource> patterns,
    int minimumOmissionDays) {
  /** Retains the captured ordered pattern values independently from caller collections. */
  public AdherenceSnapshotResource {
    patterns = List.copyOf(patterns);
  }
}
