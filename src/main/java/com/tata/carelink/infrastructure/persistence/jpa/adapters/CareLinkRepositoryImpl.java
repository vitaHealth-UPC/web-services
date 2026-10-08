package com.tata.carelink.infrastructure.persistence.jpa.adapters;

import com.tata.carelink.domain.model.aggregates.CareLink;
import com.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.tata.carelink.domain.repositories.CareLinkRepository;
import com.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkJpaRepository;
import com.tata.carelink.infrastructure.persistence.jpa.assemblers.CareLinkPersistenceAssembler;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CareLinkRepositoryImpl implements CareLinkRepository {
    private final CareLinkJpaRepository repository;

    public CareLinkRepositoryImpl(CareLinkJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public CareLink save(CareLink careLink) {
        return CareLinkPersistenceAssembler.toDomain(repository.save(CareLinkPersistenceAssembler.toEntity(careLink)));
    }

    @Override
    public java.util.List<CareLink> findConfirmedByCaregiver(String caregiverId) {
        return repository.findByCaregiverIdAndStatusOrderByConfirmedAtDescIdAsc(caregiverId, CareLinkStatus.CONFIRMED)
                .stream().map(CareLinkPersistenceAssembler::toDomain).filter(CareLink::isActive).toList();
    }

    @Override
    public Optional<CareLink> findById(String id) {
        return repository.findById(id).map(CareLinkPersistenceAssembler::toDomain);
    }

    @Override
    public Optional<CareLink> findByCode(String code) {
        return repository.findByLinkingCode(code).map(CareLinkPersistenceAssembler::toDomain);
    }

    @Override
    public Optional<CareLink> findConfirmed(String caregiverId, String olderAdultId) {
        return repository.findFirstByCaregiverIdAndOlderAdultIdAndStatus(
                caregiverId,
                olderAdultId,
                CareLinkStatus.CONFIRMED
        ).map(CareLinkPersistenceAssembler::toDomain);
    }

}
