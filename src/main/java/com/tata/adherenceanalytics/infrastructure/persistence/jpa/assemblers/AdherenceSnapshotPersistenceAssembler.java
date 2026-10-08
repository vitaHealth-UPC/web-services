package com.tata.adherenceanalytics.infrastructure.persistence.jpa.assemblers;
import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import com.tata.adherenceanalytics.infrastructure.persistence.jpa.entities.AdherenceSnapshotPersistenceEntity;
public final class AdherenceSnapshotPersistenceAssembler {
 private AdherenceSnapshotPersistenceAssembler() {}
 public static AdherenceSnapshotPersistenceEntity toEntity(AdherencePeriodSnapshot d) {
  return new AdherenceSnapshotPersistenceEntity(d.id(),d.olderAdultId(),d.from(),d.to(),d.zone(),d.onTimeIntakes(),d.lateIntakes(),d.omittedIntakes(),d.patterns(),d.minimumOmissionDays(),d.consolidatedAt());
 }
 public static AdherencePeriodSnapshot toDomain(AdherenceSnapshotPersistenceEntity e) {
  return new AdherencePeriodSnapshot(e.id(),e.olderAdultId(),e.from(),e.to(),e.zone(),e.onTimeIntakes(),e.lateIntakes(),e.omittedIntakes(),e.patterns(),e.minimumOmissionDays(),e.consolidatedAt());
 }
}
