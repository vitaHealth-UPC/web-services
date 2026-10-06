package com.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSessionJpaRepository extends JpaRepository<AccountSessionPersistenceEntity, String> {}
