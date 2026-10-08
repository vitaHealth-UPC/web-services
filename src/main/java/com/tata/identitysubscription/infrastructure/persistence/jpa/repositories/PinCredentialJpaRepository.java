package com.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.PinCredentialPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PinCredentialJpaRepository extends JpaRepository<PinCredentialPersistenceEntity, String> {
    Optional<PinCredentialPersistenceEntity> findByOlderAdultId(String olderAdultId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PinCredentialPersistenceEntity p where p.olderAdultId = :olderAdultId")
    Optional<PinCredentialPersistenceEntity> findForAuthentication(@org.springframework.data.repository.query.Param("olderAdultId") String olderAdultId);
}
