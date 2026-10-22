package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.AdherenceInsightPersistenceEntity;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.AlertSummaryPersistenceEntity;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.CaregiverNotePersistenceEntity;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.FamilyMonitorPersistenceEntity;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.LowStockNoticePersistenceEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Updates reserved managed children without merging detached identities or erasing history. */
public final class FamilyMonitorPersistenceStateUpdater {
  private FamilyMonitorPersistenceStateUpdater() {}

  /** Validates the complete replacement before modifying any managed collection or child. */
  public static void apply(
      FamilyMonitorPersistenceEntity managed, FamilyMonitorPersistenceEntity snapshot) {
    requireSame(managed.getCareLinkId(), snapshot.getCareLinkId());
    requireSame(managed.getOlderAdultId(), snapshot.getOlderAdultId());
    requireSame(managed.getFamiliarId(), snapshot.getFamiliarId());
    unique(snapshot.getAlerts(), AlertSummaryPersistenceEntity::getIntakeId);
    unique(snapshot.getLowStockNotices(), LowStockNoticePersistenceEntity::getMedicationId);
    unique(
        snapshot.getAdherenceInsights(),
        value -> List.of(value.getMedicationId(), value.getFirstDay(), value.getLastDay()));
    var positions =
        snapshot.getInventoryStockPositions().stream().map(value -> value.toDomain()).toList();
    unique(positions, value -> value.medicationId());
    for (var stored : managed.getInventoryStockPositions()) {
      var previous = stored.toDomain();
      var incoming =
          positions.stream()
              .filter(value -> value.medicationId().equals(previous.medicationId()))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalStateException("inventory source position cannot be removed"));
      if (incoming.revision() < previous.revision())
        throw new IllegalStateException("inventory source revision cannot move backwards");
      incoming.advances(previous);
    }
    var alerts =
        resolve(
            managed.getAlerts(),
            snapshot.getAlerts(),
            AlertSummaryPersistenceEntity::getId,
            false,
            FamilyMonitorPersistenceStateUpdater::validateAlert);
    var notes =
        resolve(
            managed.getNotes(),
            snapshot.getNotes(),
            CaregiverNotePersistenceEntity::getId,
            false,
            FamilyMonitorPersistenceStateUpdater::validateNote);
    var stock =
        resolve(
            managed.getLowStockNotices(),
            snapshot.getLowStockNotices(),
            LowStockNoticePersistenceEntity::getId,
            true,
            FamilyMonitorPersistenceStateUpdater::validateStock);
    var insights =
        resolve(
            managed.getAdherenceInsights(),
            snapshot.getAdherenceInsights(),
            AdherenceInsightPersistenceEntity::getId,
            false,
            FamilyMonitorPersistenceStateUpdater::validateInsight);

    for (var incoming : snapshot.getAlerts()) {
      if (incoming.getId() == null) continue;
      var target =
          alerts.stream()
              .filter(value -> incoming.getId().equals(value.getId()))
              .findFirst()
              .orElseThrow();
      target.setStatus(incoming.getStatus());
      target.setClosedAt(incoming.getClosedAt());
    }
    for (var incoming : snapshot.getLowStockNotices()) {
      if (incoming.getId() == null) continue;
      var target =
          stock.stream()
              .filter(value -> incoming.getId().equals(value.getId()))
              .findFirst()
              .orElseThrow();
      target.setMedicationName(incoming.getMedicationName());
      target.setRemainingStock(incoming.getRemainingStock());
      target.setReplenishmentThreshold(incoming.getReplenishmentThreshold());
      target.setDetectedAt(incoming.getDetectedAt());
    }
    replace(managed.getInventoryStockPositions(), snapshot.getInventoryStockPositions());
    replace(managed.getAlerts(), alerts);
    replace(managed.getNotes(), notes);
    replace(managed.getLowStockNotices(), stock);
    replace(managed.getAdherenceInsights(), insights);
  }

  private static <T> List<T> resolve(
      List<T> existing,
      List<T> incoming,
      Function<T, Long> identity,
      boolean allowRemoval,
      BiConsumer<T, T> validate) {
    Map<Long, T> owned = new HashMap<>();
    for (T value : existing) {
      Long id = identity.apply(value);
      if (id == null || owned.put(id, value) != null)
        throw new IllegalStateException("stored follow-up child identities are inconsistent");
    }
    var seen = new HashSet<Long>();
    var resolved = new ArrayList<T>();
    for (T value : incoming) {
      Objects.requireNonNull(value, "follow-up child");
      Long id = identity.apply(value);
      if (id == null) {
        validate.accept(null, value);
        resolved.add(value);
      } else {
        T current = owned.get(id);
        if (current == null || !seen.add(id))
          throw new IllegalStateException("follow-up child identity is not uniquely owned");
        validate.accept(current, value);
        resolved.add(current);
      }
    }
    if (!allowRemoval && seen.size() != owned.size())
      throw new IllegalStateException("follow-up history cannot be removed");
    return resolved;
  }

  private static void validateAlert(
      AlertSummaryPersistenceEntity old, AlertSummaryPersistenceEntity value) {
    Objects.requireNonNull(value.getStatus(), "alert status");
    Objects.requireNonNull(value.getOpenedAt(), "alert occurrence");
    Objects.requireNonNull(value.getScheduledAt(), "alert schedule");
    if (value.getStatus() == AlertStatus.CLOSED) {
      if (value.getClosedAt() == null || value.getClosedAt().isBefore(value.getOpenedAt()))
        throw new IllegalStateException("stored alert closure is inconsistent");
    } else if (value.getClosedAt() != null) {
      throw new IllegalStateException("unclosed alert contains closure evidence");
    }
    if (old == null) return;
    requireSame(old.getIntakeId(), value.getIntakeId());
    requireSame(old.getMedicationName(), value.getMedicationName());
    requireSame(old.getScheduledAt(), value.getScheduledAt());
    requireSame(old.getReason(), value.getReason());
    requireSame(old.getOpenedAt(), value.getOpenedAt());
    if (old.getStatus() == AlertStatus.CLOSED) {
      requireSame(old.getStatus(), value.getStatus());
      requireSame(old.getClosedAt(), value.getClosedAt());
    } else if (old.getStatus() == AlertStatus.ATTENDED && value.getStatus() == AlertStatus.OPEN) {
      throw new IllegalStateException("attended alert cannot reopen");
    }
  }

  private static void validateNote(
      CaregiverNotePersistenceEntity old, CaregiverNotePersistenceEntity value) {
    Objects.requireNonNull(value.getText(), "note text");
    Objects.requireNonNull(value.getRecordedAt(), "note occurrence");
    Objects.requireNonNull(value.getFamiliarId(), "note author");
    if (old == null) return;
    requireSame(old.getText(), value.getText());
    requireSame(old.getRecordedAt(), value.getRecordedAt());
    requireSame(old.getFamiliarId(), value.getFamiliarId());
  }

  private static void validateStock(
      LowStockNoticePersistenceEntity old, LowStockNoticePersistenceEntity value) {
    Objects.requireNonNull(value.getDetectedAt(), "stock detection time");
    if (value.getRemainingStock() < 0 || value.getReplenishmentThreshold() < 0)
      throw new IllegalStateException("stock quantities cannot be negative");
    if (old == null) return;
    requireSame(old.getMedicationId(), value.getMedicationId());
    if (value.getDetectedAt().isBefore(old.getDetectedAt()))
      throw new IllegalStateException("stock detection time cannot move backwards");
  }

  private static void validateInsight(
      AdherenceInsightPersistenceEntity old, AdherenceInsightPersistenceEntity value) {
    Objects.requireNonNull(value.getDetectedAt(), "insight occurrence");
    if (old == null) return;
    requireSame(old.getMedicationId(), value.getMedicationId());
    requireSame(old.getMedicationName(), value.getMedicationName());
    requireSame(old.getOmissionDays(), value.getOmissionDays());
    requireSame(old.getFirstDay(), value.getFirstDay());
    requireSame(old.getLastDay(), value.getLastDay());
    requireSame(old.getDetectedAt(), value.getDetectedAt());
  }

  private static <T> void unique(List<T> values, Function<T, ?> key) {
    var seen = new HashSet<Object>();
    for (T value : values) {
      Object logicalKey = Objects.requireNonNull(key.apply(value), "follow-up child reference");
      if (!seen.add(logicalKey))
        throw new IllegalStateException("duplicate follow-up child reference");
    }
  }

  private static void requireSame(Object before, Object after) {
    if (!Objects.equals(before, after))
      throw new IllegalStateException("original follow-up evidence cannot change");
  }

  private static <T> void replace(List<T> managed, List<T> values) {
    managed.clear();
    managed.addAll(values);
  }
}
