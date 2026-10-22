package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.entities.CareAlert;
import com.vitahealth.tata.omissionescalation.domain.model.entities.EscalationRecord;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.EscalationLevel;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.embeddables.EscalationLevelPersistenceEmbeddable;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.embeddables.GracePeriodPersistenceEmbeddable;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.CareAlertPersistenceEntity;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.EscalationRecordPersistenceEntity;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.OmissionCasePersistenceEntity;
import java.util.ArrayList;
import java.util.stream.Collectors;

/** Converts one omission aggregate and its owned history without performing storage or delivery. */
public final class OmissionCasePersistenceAssembler {
  private OmissionCasePersistenceAssembler() {}

  /** Copies complete aggregate evidence and child identities into a persistence representation. */
  public static OmissionCasePersistenceEntity toEntity(OmissionCase domain) {
    var entity = new OmissionCasePersistenceEntity();
    entity.setId(domain.getId());
    entity.setIntakeId(domain.getIntakeId());
    entity.setOlderAdultId(domain.getOlderAdultId());
    entity.setMedicationName(domain.getMedicationName());
    entity.setScheduledAt(domain.getScheduledAt());
    entity.setStatus(domain.getStatus());
    entity.setGracePeriod(
        new GracePeriodPersistenceEmbeddable(
            domain.getGracePeriod().startsAt(), domain.getGracePeriod().endsAt()));
    entity.setReinforcedReminderSentAt(domain.getReinforcedReminderSentAt());
    entity.setOmittedAt(domain.getOmittedAt());
    entity.setClosedAt(domain.getClosedAt());
    entity.setAlerts(
        domain.getAlerts().stream()
            .map(OmissionCasePersistenceAssembler::toEntity)
            .collect(Collectors.toCollection(ArrayList::new)));
    entity.setEscalations(
        domain.getEscalations().stream()
            .map(OmissionCasePersistenceAssembler::toEntity)
            .collect(Collectors.toCollection(ArrayList::new)));
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    return entity;
  }

  /**
   * Restores aggregate evidence and validated escalation order without replaying business events.
   */
  public static OmissionCase toDomain(OmissionCasePersistenceEntity entity) {
    try {
      return restore(entity);
    } catch (IllegalArgumentException | IllegalStateException | NullPointerException invalidState) {
      throw new IllegalStateException("stored omission case is inconsistent", invalidState);
    }
  }

  private static OmissionCase restore(OmissionCasePersistenceEntity entity) {
    return OmissionCase.rehydrate(
        entity.getId(),
        entity.getIntakeId(),
        entity.getOlderAdultId(),
        entity.getMedicationName(),
        entity.getScheduledAt(),
        entity.getStatus(),
        new GracePeriod(entity.getGracePeriod().startsAt(), entity.getGracePeriod().endsAt()),
        entity.getReinforcedReminderSentAt(),
        entity.getOmittedAt(),
        entity.getClosedAt(),
        entity.getAlerts().stream().map(OmissionCasePersistenceAssembler::toDomain).toList(),
        entity.getEscalations().stream().map(OmissionCasePersistenceAssembler::toDomain).toList(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  /** Copies the alert delivery evidence, retaining its stored identity when present. */
  public static CareAlertPersistenceEntity toEntity(CareAlert domain) {
    var entity = new CareAlertPersistenceEntity();
    entity.setId(domain.getId());
    entity.setStatus(domain.getStatus());
    entity.setGeneratedAt(domain.getGeneratedAt());
    entity.setSentAt(domain.getSentAt());
    entity.setFailureReason(domain.getFailureReason());
    return entity;
  }

  /** Restores coherent alert evidence; contradictory saved state is a technical failure. */
  public static CareAlert toDomain(CareAlertPersistenceEntity entity) {
    try {
      return CareAlert.rehydrate(
          entity.getId(),
          entity.getStatus(),
          entity.getGeneratedAt(),
          entity.getSentAt(),
          entity.getFailureReason());
    } catch (IllegalArgumentException | NullPointerException invalidState) {
      throw new IllegalStateException("stored care alert is inconsistent", invalidState);
    }
  }

  /** Copies the original escalation level, reason, time and identity. */
  public static EscalationRecordPersistenceEntity toEntity(EscalationRecord domain) {
    var entity = new EscalationRecordPersistenceEntity();
    entity.setId(domain.getId());
    entity.setLevel(new EscalationLevelPersistenceEmbeddable(domain.getLevel().value()));
    entity.setReason(domain.getReason());
    entity.setTriggeredAt(domain.getTriggeredAt());
    return entity;
  }

  /** Restores one escalation record; contradictory saved evidence is a technical failure. */
  public static EscalationRecord toDomain(EscalationRecordPersistenceEntity entity) {
    try {
      return EscalationRecord.rehydrate(
          entity.getId(),
          new EscalationLevel(entity.getLevel().value()),
          entity.getReason(),
          entity.getTriggeredAt());
    } catch (IllegalArgumentException | NullPointerException invalidState) {
      throw new IllegalStateException("stored escalation record is inconsistent", invalidState);
    }
  }
}
