package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AdherenceInsight;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.vitahealth.tata.familymonitoring.domain.model.entities.LowStockNotice;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.AdherenceInsightPersistenceEntity;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.AlertSummaryPersistenceEntity;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.CaregiverNotePersistenceEntity;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.FamilyMonitorPersistenceEntity;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.InventoryStockPositionEmbeddable;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.LowStockNoticePersistenceEntity;
import java.util.ArrayList;
import java.util.stream.Collectors;

/** Maps complete Family Monitoring snapshots without executing business transitions. */
public final class FamilyMonitorPersistenceAssembler {
  private FamilyMonitorPersistenceAssembler() {}

  /** Copies original business evidence and its stored identities between representations. */
  public static FamilyMonitorPersistenceEntity toEntity(FamilyMonitor domain) {
    var entity = new FamilyMonitorPersistenceEntity();
    entity.setId(domain.getId());
    entity.setRevision(domain.getRevision());
    entity.setCareLinkId(domain.getCareLinkId());
    entity.setOlderAdultId(domain.getOlderAdultId());
    entity.setFamiliarId(domain.getFamiliarId());
    entity.setAlerts(
        domain.getAlerts().stream()
            .map(FamilyMonitorPersistenceAssembler::toEntity)
            .collect(Collectors.toCollection(ArrayList::new)));
    entity.setNotes(
        domain.getNotes().stream()
            .map(FamilyMonitorPersistenceAssembler::toEntity)
            .collect(Collectors.toCollection(ArrayList::new)));
    entity.setLowStockNotices(
        domain.getLowStockNotices().stream()
            .map(FamilyMonitorPersistenceAssembler::toEntity)
            .collect(Collectors.toCollection(ArrayList::new)));
    entity.setAdherenceInsights(
        domain.getAdherenceInsights().stream()
            .map(FamilyMonitorPersistenceAssembler::toEntity)
            .collect(Collectors.toCollection(ArrayList::new)));
    entity.setInventoryStockPositions(
        domain.getInventoryStockPositions().stream()
            .map(InventoryStockPositionEmbeddable::new)
            .toList());
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    return entity;
  }

  /** Copies original business evidence and its stored identities between representations. */
  public static FamilyMonitor toDomain(FamilyMonitorPersistenceEntity entity) {
    return FamilyMonitor.rehydrate(
        entity.getId(),
        entity.getCareLinkId(),
        entity.getOlderAdultId(),
        entity.getFamiliarId(),
        entity.getAlerts().stream().map(FamilyMonitorPersistenceAssembler::toDomain).toList(),
        entity.getNotes().stream().map(FamilyMonitorPersistenceAssembler::toDomain).toList(),
        entity.getLowStockNotices().stream()
            .map(FamilyMonitorPersistenceAssembler::toDomain)
            .toList(),
        entity.getAdherenceInsights().stream()
            .map(FamilyMonitorPersistenceAssembler::toDomain)
            .toList(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getRevision(),
        entity.getInventoryStockPositions().stream()
            .map(InventoryStockPositionEmbeddable::toDomain)
            .toList());
  }

  /** Copies original business evidence and its stored identities between representations. */
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

  /** Copies original business evidence and its stored identities between representations. */
  public static AlertSummary toDomain(AlertSummaryPersistenceEntity entity) {
    return AlertSummary.rehydrate(
        entity.getId(),
        entity.getIntakeId(),
        entity.getMedicationName(),
        entity.getScheduledAt(),
        entity.getReason(),
        entity.getStatus(),
        entity.getOpenedAt(),
        entity.getClosedAt());
  }

  /** Copies original business evidence and its stored identities between representations. */
  public static CaregiverNotePersistenceEntity toEntity(CaregiverNote domain) {
    var entity = new CaregiverNotePersistenceEntity();
    entity.setId(domain.getId());
    entity.setText(domain.getText());
    entity.setRecordedAt(domain.getRecordedAt());
    entity.setFamiliarId(domain.getFamiliarId());
    return entity;
  }

  /** Copies original business evidence and its stored identities between representations. */
  public static CaregiverNote toDomain(CaregiverNotePersistenceEntity entity) {
    return CaregiverNote.rehydrate(
        entity.getId(), entity.getText(), entity.getRecordedAt(), entity.getFamiliarId());
  }

  /** Copies original business evidence and its stored identities between representations. */
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

  /** Copies original business evidence and its stored identities between representations. */
  public static LowStockNotice toDomain(LowStockNoticePersistenceEntity entity) {
    return LowStockNotice.rehydrate(
        entity.getId(),
        entity.getMedicationId(),
        entity.getMedicationName(),
        entity.getRemainingStock(),
        entity.getReplenishmentThreshold(),
        entity.getDetectedAt());
  }

  /** Copies original business evidence and its stored identities between representations. */
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

  /** Copies original business evidence and its stored identities between representations. */
  public static AdherenceInsight toDomain(AdherenceInsightPersistenceEntity entity) {
    return AdherenceInsight.rehydrate(
        entity.getId(),
        entity.getMedicationId(),
        entity.getMedicationName(),
        entity.getOmissionDays(),
        entity.getFirstDay(),
        entity.getLastDay(),
        entity.getDetectedAt());
  }
}
