package com.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.tata.identitysubscription.domain.model.aggregates.PinCredential;
import com.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.PinCredentialPersistenceEntity;
import com.tata.identitysubscription.infrastructure.persistence.jpa.repositories.PinCredentialJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PinCredentialRepositoryImpl implements PinCredentialRepository {
    private final PinCredentialJpaRepository repository;

    public PinCredentialRepositoryImpl(PinCredentialJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PinCredential> findByOlderAdultId(String olderAdultId) {
        return repository.findByOlderAdultId(olderAdultId).map(this::toDomain);
    }

    @Override
    public PinCredential save(PinCredential credential) {
        return toDomain(repository.save(toEntity(credential)));
    }

    private PinCredential toDomain(PinCredentialPersistenceEntity entity) {
        return PinCredential.rehydrate(
                entity.getId(), entity.getOlderAdultId(), entity.getPinHash(),
                entity.getFailedAttempts(), entity.getLockedUntil()
        );
    }

    private PinCredentialPersistenceEntity toEntity(PinCredential credential) {
        return new PinCredentialPersistenceEntity(
                credential.id(), credential.olderAdultId(), credential.pinHash(),
                credential.failedAttempts(), credential.lockedUntil()
        );
    }
}
