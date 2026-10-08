package com.tata.accessibilitypreferences.infrastructure.persistence.jpa.repositories;

import com.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities.UserPreferencesPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferencesJpaRepository extends JpaRepository<UserPreferencesPersistenceEntity, String> {
    Optional<UserPreferencesPersistenceEntity> findByUserId(String userId);
}
