package com.tata.carelink.infrastructure.persistence.jpa.adapters;

import com.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import com.tata.carelink.infrastructure.persistence.jpa.repositories.OlderAdultProfileJpaRepository;
import com.tata.carelink.infrastructure.persistence.jpa.assemblers.OlderAdultProfilePersistenceAssembler;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OlderAdultProfileRepositoryImpl implements OlderAdultProfileRepository {
    private final OlderAdultProfileJpaRepository repository;

    public OlderAdultProfileRepositoryImpl(OlderAdultProfileJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OlderAdultProfile save(OlderAdultProfile profile) {
        return OlderAdultProfilePersistenceAssembler.toDomain(repository.save(OlderAdultProfilePersistenceAssembler.toEntity(profile)));
    }

    @Override
    public Optional<OlderAdultProfile> findById(String id) {
        return repository.findById(id).map(OlderAdultProfilePersistenceAssembler::toDomain);
    }

}
