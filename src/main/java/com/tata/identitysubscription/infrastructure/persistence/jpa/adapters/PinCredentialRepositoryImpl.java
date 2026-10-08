package com.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.tata.identitysubscription.domain.model.aggregates.PinCredential;
import com.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import com.tata.identitysubscription.infrastructure.persistence.jpa.repositories.PinCredentialJpaRepository;
import com.tata.identitysubscription.infrastructure.persistence.jpa.assemblers.PinCredentialPersistenceAssembler;
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
        return repository.findByOlderAdultId(olderAdultId).map(PinCredentialPersistenceAssembler::toDomain);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Optional<PinCredential> findForAuthentication(String olderAdultId) {
        return repository.findForAuthentication(olderAdultId).map(PinCredentialPersistenceAssembler::toDomain);
    }
    @Override
    public PinCredential save(PinCredential credential) {
        return PinCredentialPersistenceAssembler.toDomain(repository.save(PinCredentialPersistenceAssembler.toEntity(credential)));
    }

}
