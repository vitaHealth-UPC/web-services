package com.tata.omissionescalation.infrastructure.persistence.jpa.assemblers;

import com.tata.omissionescalation.domain.model.aggregates.*;
import com.tata.omissionescalation.domain.model.entities.*;
import com.tata.omissionescalation.infrastructure.persistence.jpa.entities.*;
import com.tata.omissionescalation.infrastructure.persistence.jpa.embeddables.*;

public final class PersistenceAssembler {
  private PersistenceAssembler() {}
  public static OmissionCasePersistenceEntity toEntity(OmissionCase domain) {
    var entity = new OmissionCasePersistenceEntity();
    entity.setId(domain.getId());
    entity.setIntakeId(domain.getIntakeId());
    entity.setOlderAdultId(domain.getOlderAdultId());
    entity.setMedicationName(domain.getMedicationName());
    entity.setScheduledAt(domain.getScheduledAt());
    entity.setStatus(domain.getStatus());
    entity.setGracePeriod(new GracePeriodPersistenceEmbeddable(domain.getGracePeriod().startsAt(), domain.getGracePeriod().endsAt()));
    entity.setReinforcedReminderSentAt(domain.getReinforcedReminderSentAt());
    entity.setOmittedAt(domain.getOmittedAt());
    entity.setClosedAt(domain.getClosedAt());
    entity.setAlerts(domain.getAlerts().stream().map(PersistenceAssembler::toEntity).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
    entity.setEscalations(domain.getEscalations().stream().map(PersistenceAssembler::toEntity).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    return entity;
  }
  public static OmissionCase toDomain(OmissionCasePersistenceEntity entity) {
    return OmissionCase.rehydrate(entity.getId(), entity.getIntakeId(), entity.getOlderAdultId(), entity.getMedicationName(), entity.getScheduledAt(), entity.getStatus(), new com.tata.omissionescalation.domain.model.valueobjects.GracePeriod(entity.getGracePeriod().startsAt(), entity.getGracePeriod().endsAt()), entity.getReinforcedReminderSentAt(), entity.getOmittedAt(), entity.getClosedAt(), entity.getAlerts().stream().map(PersistenceAssembler::toDomain).toList(), entity.getEscalations().stream().map(PersistenceAssembler::toDomain).toList(), entity.getCreatedAt(), entity.getUpdatedAt());
  }

  public static CareAlertPersistenceEntity toEntity(CareAlert domain) {
    var entity = new CareAlertPersistenceEntity();
    entity.setId(domain.getId());
    entity.setStatus(domain.getStatus());
    entity.setGeneratedAt(domain.getGeneratedAt());
    entity.setSentAt(domain.getSentAt());
    entity.setFailureReason(domain.getFailureReason());
    return entity;
  }
  public static CareAlert toDomain(CareAlertPersistenceEntity entity) {
    return CareAlert.rehydrate(entity.getId(), entity.getStatus(), entity.getGeneratedAt(), entity.getSentAt(), entity.getFailureReason());
  }

  public static EscalationRecordPersistenceEntity toEntity(EscalationRecord domain) {
    var entity = new EscalationRecordPersistenceEntity();
    entity.setId(domain.getId());
    entity.setLevel(new EscalationLevelPersistenceEmbeddable(domain.getLevel().value()));
    entity.setReason(domain.getReason());
    entity.setTriggeredAt(domain.getTriggeredAt());
    return entity;
  }
  public static EscalationRecord toDomain(EscalationRecordPersistenceEntity entity) {
    return EscalationRecord.rehydrate(entity.getId(), new com.tata.omissionescalation.domain.model.valueobjects.EscalationLevel(entity.getLevel().value()), entity.getReason(), entity.getTriggeredAt());
  }
}
