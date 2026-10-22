package com.vitahealth.tata.adherenceanalytics.interfaces.rest.transform;

import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.resources.AdherenceSnapshotResource;

/** Maps original snapshot evidence to its dedicated transport representation. */
public final class AdherenceSnapshotResourceAssembler {
  private AdherenceSnapshotResourceAssembler() {}

  /** Preserves the original consolidation instant and captured pattern order. */
  public static AdherenceSnapshotResource from(AdherencePeriodSnapshot snapshot) {
    return new AdherenceSnapshotResource(
        snapshot.id(),
        snapshot.olderAdultId(),
        snapshot.from(),
        snapshot.to(),
        snapshot.zone(),
        snapshot.consolidatedAt(),
        snapshot.confirmedIntakes(),
        snapshot.totalIntakes(),
        snapshot.percentage(),
        snapshot.onTimeIntakes(),
        snapshot.lateIntakes(),
        snapshot.omittedIntakes(),
        snapshot.patterns().stream().map(AdherencePatternResourceAssembler::from).toList(),
        snapshot.minimumOmissionDays());
  }
}
