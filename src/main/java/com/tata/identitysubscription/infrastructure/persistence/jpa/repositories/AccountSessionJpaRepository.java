package com.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSessionJpaRepository extends JpaRepository<AccountSessionPersistenceEntity, String> {
 java.util.Optional<AccountSessionPersistenceEntity> findByTokenHash(String hash);
 void deleteByTokenHash(String hash);
 java.util.Optional<AccountSessionPersistenceEntity> findByTokenHashAndExpiresAtAfter(String hash, java.time.Instant now);
}
