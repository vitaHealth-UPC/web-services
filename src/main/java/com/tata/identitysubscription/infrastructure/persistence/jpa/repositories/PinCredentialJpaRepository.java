package com.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.PinCredentialPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PinCredentialJpaRepository extends JpaRepository<PinCredentialPersistenceEntity, String> {
    Optional<PinCredentialPersistenceEntity> findByOlderAdultId(String olderAdultId);
}
