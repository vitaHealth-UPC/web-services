package com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionPattern;
import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.embeddables.AdherencePatternEmbeddable;
import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.entities.AdherenceSnapshotPersistenceEntity;

/** Maps immutable domain evidence to owned JPA state without recalculating history. */
public final class AdherenceSnapshotPersistenceAssembler {
  private AdherenceSnapshotPersistenceAssembler() {}

  /** Preserves all captured values and pattern order for insertion. */
  public static AdherenceSnapshotPersistenceEntity toEntity(AdherencePeriodSnapshot snapshot) {
    return new AdherenceSnapshotPersistenceEntity(
        snapshot.id(),
        snapshot.olderAdultId(),
        snapshot.from(),
        snapshot.to(),
        snapshot.zone(),
        snapshot.onTimeIntakes(),
        snapshot.lateIntakes(),
        snapshot.omittedIntakes(),
        snapshot.patterns().stream()
            .map(
                pattern ->
                    new AdherencePatternEmbeddable(
                        pattern.medicationId(),
                        pattern.omissionDays(),
                        pattern.firstDay(),
                        pattern.lastDay()))
            .toList(),
        snapshot.minimumOmissionDays(),
        snapshot.consolidatedAt());
  }

  /** Restores original evidence through domain invariants. */
  public static AdherencePeriodSnapshot toDomain(AdherenceSnapshotPersistenceEntity entity) {
    return new AdherencePeriodSnapshot(
        entity.id(),
        entity.olderAdultId(),
        entity.from(),
        entity.to(),
        entity.zone(),
        entity.onTimeIntakes(),
        entity.lateIntakes(),
        entity.omittedIntakes(),
        entity.patterns().stream()
            .map(
                pattern ->
                    new AdherenceOmissionPattern(
                        pattern.medicationId(),
                        pattern.omissionDays(),
                        pattern.firstDay(),
                        pattern.lastDay()))
            .toList(),
        entity.minimumOmissionDays(),
        entity.consolidatedAt());
  }
}
