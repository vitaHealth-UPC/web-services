package com.tata.carelink.infrastructure.persistence.jpa.repositories;

import com.tata.carelink.infrastructure.persistence.jpa.entities.OlderAdultProfilePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OlderAdultProfileJpaRepository extends JpaRepository<OlderAdultProfilePersistenceEntity, String> {
}
