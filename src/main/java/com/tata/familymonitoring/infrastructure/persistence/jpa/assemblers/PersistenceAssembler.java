package com.tata.familymonitoring.infrastructure.persistence.jpa.assemblers;

import com.tata.familymonitoring.domain.model.aggregates.*;
import com.tata.familymonitoring.domain.model.entities.*;
import com.tata.familymonitoring.infrastructure.persistence.jpa.entities.*;

public final class PersistenceAssembler {
  private PersistenceAssembler() {}
  public static FamilyMonitorPersistenceEntity toEntity(FamilyMonitor domain) {
    var entity = new FamilyMonitorPersistenceEntity();
    entity.setId(domain.getId());
    entity.setCareLinkId(domain.getCareLinkId());
    entity.setOlderAdultId(domain.getOlderAdultId());
    entity.setFamiliarId(domain.getFamiliarId());
    entity.setAlerts(domain.getAlerts().stream().map(PersistenceAssembler::toEntity).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
    entity.setNotes(domain.getNotes().stream().map(PersistenceAssembler::toEntity).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
    entity.setLowStockNotices(domain.getLowStockNotices().stream().map(PersistenceAssembler::toEntity).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
    entity.setAdherenceInsights(domain.getAdherenceInsights().stream().map(PersistenceAssembler::toEntity).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    return entity;
  }
  public static FamilyMonitor toDomain(FamilyMonitorPersistenceEntity entity) {
    return FamilyMonitor.rehydrate(entity.getId(), entity.getCareLinkId(), entity.getOlderAdultId(), entity.getFamiliarId(), entity.getAlerts().stream().map(PersistenceAssembler::toDomain).toList(), entity.getNotes().stream().map(PersistenceAssembler::toDomain).toList(), entity.getLowStockNotices().stream().map(PersistenceAssembler::toDomain).toList(), entity.getAdherenceInsights().stream().map(PersistenceAssembler::toDomain).toList(), entity.getCreatedAt(), entity.getUpdatedAt());
  }

  public static AlertSummaryPersistenceEntity toEntity(AlertSummary domain) {
    var entity = new AlertSummaryPersistenceEntity();
    entity.setId(domain.getId());
    entity.setIntakeId(domain.getIntakeId());
    entity.setMedicationName(domain.getMedicationName());
    entity.setScheduledAt(domain.getScheduledAt());
    entity.setReason(domain.getReason());
    entity.setStatus(domain.getStatus());
    entity.setOpenedAt(domain.getOpenedAt());
    entity.setClosedAt(domain.getClosedAt());
    return entity;
  }
  public static AlertSummary toDomain(AlertSummaryPersistenceEntity entity) {
    return AlertSummary.rehydrate(entity.getId(), entity.getIntakeId(), entity.getMedicationName(), entity.getScheduledAt(), entity.getReason(), entity.getStatus(), entity.getOpenedAt(), entity.getClosedAt());
  }

  public static CaregiverNotePersistenceEntity toEntity(CaregiverNote domain) {
    var entity = new CaregiverNotePersistenceEntity();
    entity.setId(domain.getId());
    entity.setText(domain.getText());
    entity.setRecordedAt(domain.getRecordedAt());
    entity.setFamiliarId(domain.getFamiliarId());
    return entity;
  }
  public static CaregiverNote toDomain(CaregiverNotePersistenceEntity entity) {
    return CaregiverNote.rehydrate(entity.getId(), entity.getText(), entity.getRecordedAt(), entity.getFamiliarId());
  }

  public static LowStockNoticePersistenceEntity toEntity(LowStockNotice domain) {
    var entity = new LowStockNoticePersistenceEntity();
    entity.setId(domain.getId());
    entity.setMedicationId(domain.getMedicationId());
    entity.setMedicationName(domain.getMedicationName());
    entity.setRemainingStock(domain.getRemainingStock());
    entity.setReplenishmentThreshold(domain.getReplenishmentThreshold());
    entity.setDetectedAt(domain.getDetectedAt());
    return entity;
  }
  public static LowStockNotice toDomain(LowStockNoticePersistenceEntity entity) {
    return LowStockNotice.rehydrate(entity.getId(), entity.getMedicationId(), entity.getMedicationName(), entity.getRemainingStock(), entity.getReplenishmentThreshold(), entity.getDetectedAt());
  }

  public static AdherenceInsightPersistenceEntity toEntity(AdherenceInsight domain) {
    var entity = new AdherenceInsightPersistenceEntity();
    entity.setId(domain.getId());
    entity.setMedicationId(domain.getMedicationId());
    entity.setMedicationName(domain.getMedicationName());
    entity.setOmissionDays(domain.getOmissionDays());
    entity.setFirstDay(domain.getFirstDay());
    entity.setLastDay(domain.getLastDay());
    entity.setDetectedAt(domain.getDetectedAt());
    return entity;
  }
  public static AdherenceInsight toDomain(AdherenceInsightPersistenceEntity entity) {
    return AdherenceInsight.rehydrate(entity.getId(), entity.getMedicationId(), entity.getMedicationName(), entity.getOmissionDays(), entity.getFirstDay(), entity.getLastDay(), entity.getDetectedAt());
  }
}
