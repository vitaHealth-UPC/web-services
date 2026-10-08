package com.tata.treatmentmanagement.infrastructure.persistence.jpa.assemblers;

import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.TreatmentPersistenceEntity;

public final class TreatmentPersistenceAssembler {
    private TreatmentPersistenceAssembler() {}

    public static Treatment toDomain(TreatmentPersistenceEntity entity) {
        TreatmentRegimen regimen = null;
        if (entity.getMedicationId() != null) {
            regimen = new TreatmentRegimen(
                    entity.getMedicationId(),
                    entity.getDose(),
                    entity.getFrequency(),
                    entity.getScheduledTimes(),
                    entity.getInstructions(),
                    entity.getReminderLeadMinutes() == null ? 0 : entity.getReminderLeadMinutes()
            );
        }
        return Treatment.rehydrate(
                entity.getId(), entity.getOlderAdultId(), entity.getName(), entity.getStatus(),
                regimen, entity.getCreatedAt()
        );
    }

    public static TreatmentPersistenceEntity toEntity(Treatment treatment) {
        var regimen = treatment.regimen();
        return new TreatmentPersistenceEntity(
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
                treatment.createdAt()
        );
    }
}
