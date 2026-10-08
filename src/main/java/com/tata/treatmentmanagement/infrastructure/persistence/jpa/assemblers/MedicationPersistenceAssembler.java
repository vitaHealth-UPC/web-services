package com.tata.treatmentmanagement.infrastructure.persistence.jpa.assemblers;

import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.MedicationPersistenceEntity;

/** Converts Medication state between domain and JPA representations without database access. */
public final class MedicationPersistenceAssembler {
    private MedicationPersistenceAssembler() {}

    /**
     * Restores the stored identity and state without executing a business transition.
     * @param entity stored JPA representation
     * @return reconstructed domain object
     */
    public static Medication toDomain(MedicationPersistenceEntity entity) {
        return Medication.rehydrate(
                entity.getId(), entity.getOlderAdultId(), entity.getName(), entity.getPresentation(),
                entity.isActive(), entity.getCreatedAt()
        );
    }

    /**
     * Builds the persistence representation while preserving the domain identity.
     * @return JPA state ready for the repository adapter
     */
    public static MedicationPersistenceEntity toEntity(Medication medication) {
        return new MedicationPersistenceEntity(
                medication.id(), medication.olderAdultId(), medication.name(), medication.presentation(),
                medication.active(), medication.createdAt()
        );
    }
}
