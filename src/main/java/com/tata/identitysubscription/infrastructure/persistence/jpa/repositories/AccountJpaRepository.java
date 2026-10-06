package com.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountPersistenceEntity, String> {
    Optional<AccountPersistenceEntity> findByEmail(String email);
}
