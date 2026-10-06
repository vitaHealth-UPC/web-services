package com.tata.carelink.infrastructure.persistence.jpa.repositories;

import com.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.tata.carelink.infrastructure.persistence.jpa.entities.CareLinkPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CareLinkJpaRepository extends JpaRepository<CareLinkPersistenceEntity, String> {
    Optional<CareLinkPersistenceEntity> findByLinkingCode(String linkingCode);
    Optional<CareLinkPersistenceEntity> findFirstByCaregiverIdAndOlderAdultIdAndStatus(
            String caregiverId,
            String olderAdultId,
            CareLinkStatus status
    );
}
