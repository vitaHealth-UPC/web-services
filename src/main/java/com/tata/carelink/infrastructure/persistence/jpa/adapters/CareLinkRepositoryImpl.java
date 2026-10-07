package com.tata.carelink.infrastructure.persistence.jpa.adapters;

import com.tata.carelink.domain.model.aggregates.CareLink;
import com.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.tata.carelink.domain.model.valueobjects.Consent;
import com.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.tata.carelink.domain.repositories.CareLinkRepository;
import com.tata.carelink.infrastructure.persistence.jpa.entities.CareLinkPersistenceEntity;
import com.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkJpaRepository;
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
        return toDomain(repository.save(toEntity(careLink)));
    }

    @Override
    public java.util.List<CareLink> findConfirmedByCaregiver(String caregiverId) {
        return repository.findByCaregiverIdAndStatusOrderByConfirmedAtDescIdAsc(caregiverId, CareLinkStatus.CONFIRMED)
                .stream().map(this::toDomain).filter(CareLink::isActive).toList();
    }

    @Override
    public Optional<CareLink> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CareLink> findByCode(String code) {
        return repository.findByLinkingCode(code).map(this::toDomain);
    }

    @Override
    public Optional<CareLink> findConfirmed(String caregiverId, String olderAdultId) {
        return repository.findFirstByCaregiverIdAndOlderAdultIdAndStatus(
                caregiverId,
                olderAdultId,
                CareLinkStatus.CONFIRMED
        ).map(this::toDomain);
    }

    private CareLink toDomain(CareLinkPersistenceEntity entity) {
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

    private CareLinkPersistenceEntity toEntity(CareLink careLink) {
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
