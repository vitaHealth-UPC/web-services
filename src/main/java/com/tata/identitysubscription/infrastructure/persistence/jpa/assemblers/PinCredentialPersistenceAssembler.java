package com.tata.identitysubscription.infrastructure.persistence.jpa.assemblers;

import com.tata.identitysubscription.domain.model.aggregates.PinCredential;
import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.PinCredentialPersistenceEntity;

public final class PinCredentialPersistenceAssembler {
    private PinCredentialPersistenceAssembler() {}

    public static PinCredential toDomain(PinCredentialPersistenceEntity entity) {
        return PinCredential.rehydrate(
                entity.getId(), entity.getOlderAdultId(), entity.getPinHash(),
                entity.getFailedAttempts(), entity.getLockedUntil()
        );
    }

    public static PinCredentialPersistenceEntity toEntity(PinCredential credential) {
        return new PinCredentialPersistenceEntity(
                credential.id(), credential.olderAdultId(), credential.pinHash(),
                credential.failedAttempts(), credential.lockedUntil()
        );
    }
}
