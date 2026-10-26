package com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRecurrence;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.TreatmentPersistenceEntity;

/** Converts Treatment state between domain and JPA representations without database access. */
public final class TreatmentPersistenceAssembler {
  private TreatmentPersistenceAssembler() {}

  /**
   * Restores the stored identity and state without executing a business transition.
   *
   * @param entity stored JPA representation
   * @return reconstructed domain object
   */
  public static Treatment toDomain(TreatmentPersistenceEntity entity) {
    if (entity.getMedicationId() == null
        && (entity.getDose() != null
            || entity.getFrequency() != null
            || !entity.getScheduledTimes().isEmpty()
            || entity.getReminderLeadMinutes() != null
            || entity.getFollowUpDelayMinutes() != null
            || (entity.getInstructions() != null && !entity.getInstructions().isBlank()))) {
      throw new IllegalStateException("stored treatment has regimen evidence without a medication");
    }
    if (entity.getMedicationId() == null
        && (entity.getRecurrenceMode() != null
            || entity.getRecurrenceZone() != null
            || entity.getRecurrenceAnchor() != null))
      throw new IllegalStateException("stored draft has recurrence without a regimen");
    TreatmentRegimen regimen = null;
    if (entity.getMedicationId() != null) {
      if (entity.getReminderLeadMinutes() == null) {
        throw new IllegalStateException("stored configured treatment has no reminder evidence");
      }
      regimen =
          new TreatmentRegimen(
              entity.getMedicationId(),
              entity.getDose(),
              entity.getFrequency(),
              entity.getScheduledTimes(),
              entity.getInstructions(),
              entity.getReminderLeadMinutes(),
              recurrenceFrom(entity),
              entity.getFollowUpDelayMinutes());
    }
    return Treatment.rehydrate(
        entity.getId(),
        entity.getOlderAdultId(),
        entity.getName(),
        entity.getStatus(),
        regimen,
        entity.getCreatedAt());
  }

  /**
   * Builds the persistence representation while preserving the domain identity.
   *
   * @return JPA state ready for the repository adapter
   */
  public static TreatmentPersistenceEntity toEntity(Treatment treatment) {
    var regimen = treatment.regimen();
    var entity =
        new TreatmentPersistenceEntity(
            treatment.id(),
            treatment.olderAdultId(),
            treatment.name(),
            treatment.status(),
            regimen == null ? null : regimen.medicationId(),
            regimen == null ? null : regimen.dose(),
            regimen == null ? null : regimen.frequency(),
            regimen == null ? null : regimen.scheduledTimes(),
            regimen == null ? null : regimen.instructions(),
            regimen == null ? null : regimen.reminderLeadMinutes(),
            treatment.createdAt(),
            regimen == null ? null : regimen.recurrence().mode().name(),
            regimen == null ? null : regimen.recurrence().zone(),
            regimen == null ? null : regimen.recurrence().anchor());
    entity.setFollowUpDelayMinutes(regimen == null ? null : regimen.followUpDelayMinutes());
    return entity;
  }

  /** Restores explicit evidence or the documented all-null legacy interpretation. */
  private static TreatmentRecurrence recurrenceFrom(TreatmentPersistenceEntity entity) {
    if (entity.getRecurrenceMode() == null
        && entity.getRecurrenceZone() == null
        && entity.getRecurrenceAnchor() == null) return TreatmentRecurrence.legacyUtc();
    try {
      return new TreatmentRecurrence(
          TreatmentRecurrence.Mode.valueOf(entity.getRecurrenceMode()),
          entity.getRecurrenceZone(),
          entity.getRecurrenceAnchor());
    } catch (IllegalArgumentException | NullPointerException malformed) {
      throw new IllegalStateException("stored treatment recurrence is invalid", malformed);
    }
  }
}
