package com.tata.carelink.infrastructure.persistence.jpa.assemblers;

import com.tata.carelink.domain.model.aggregates.CareLink;
import com.tata.carelink.domain.model.valueobjects.Consent;
import com.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.tata.carelink.infrastructure.persistence.jpa.entities.CareLinkPersistenceEntity;

/** Converts CareLink state between domain and JPA representations without database access. */
public final class CareLinkPersistenceAssembler {
    private CareLinkPersistenceAssembler() {}

    /**
     * Restores the stored identity and state without executing a business transition.
     * @param entity stored JPA representation
     * @return reconstructed domain object
     */
    public static CareLink toDomain(CareLinkPersistenceEntity entity) {
        return CareLink.rehydrate(
                entity.getId(),
                entity.getCaregiverId(),
                entity.getOlderAdultId(),
                entity.getStatus(),
                LinkingCode.rehydrate(entity.getLinkingCode(), entity.getCodeExpiresAt(), entity.getCodeUsedAt()),
                Consent.rehydrate(entity.isConsentGranted(), entity.getConsentRecordedAt()),
                entity.getConfirmedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Builds the persistence representation while preserving the domain identity.
     * @return JPA state ready for the repository adapter
     */
    public static CareLinkPersistenceEntity toEntity(CareLink careLink) {
        return new CareLinkPersistenceEntity(
                careLink.id(),
                careLink.caregiverId(),
                careLink.olderAdultId(),
                careLink.status(),
                careLink.linkingCode().value(),
                careLink.linkingCode().expiresAt(),
                careLink.linkingCode().usedAt(),
                careLink.consent().isGranted(),
                careLink.consent().recordedAt(),
                careLink.confirmedAt(),
                careLink.createdAt(),
                careLink.updatedAt()
        );
    }
}
