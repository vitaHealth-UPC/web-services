package com.tata.treatmentmanagement.infrastructure.persistence.jpa.assemblers;

import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.MedicationPersistenceEntity;

public final class MedicationPersistenceAssembler {
    private MedicationPersistenceAssembler() {}

    public static Medication toDomain(MedicationPersistenceEntity entity) {
        return Medication.rehydrate(
                entity.getId(), entity.getOlderAdultId(), entity.getName(), entity.getPresentation(),
                entity.isActive(), entity.getCreatedAt()
        );
    }

    public static MedicationPersistenceEntity toEntity(Medication medication) {
        return new MedicationPersistenceEntity(
                medication.id(), medication.olderAdultId(), medication.name(), medication.presentation(),
                medication.active(), medication.createdAt()
        );
    }
}
