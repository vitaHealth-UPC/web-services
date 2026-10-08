package com.tata.accessibilitypreferences.infrastructure.persistence.jpa.adapters;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import com.tata.accessibilitypreferences.infrastructure.persistence.jpa.repositories.UserPreferencesJpaRepository;
import com.tata.accessibilitypreferences.infrastructure.persistence.jpa.assemblers.UserPreferencesPersistenceAssembler;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserPreferencesRepositoryImpl implements IUserPreferencesRepository {
    private final UserPreferencesJpaRepository repository;

    public UserPreferencesRepositoryImpl(UserPreferencesJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserPreferences save(UserPreferences preferences) {
        return UserPreferencesPersistenceAssembler.toDomain(repository.saveAndFlush(UserPreferencesPersistenceAssembler.toEntity(preferences)));
    }

    @Override
    public Optional<UserPreferences> findByUserId(String userId) {
        return repository.findByUserId(userId).map(UserPreferencesPersistenceAssembler::toDomain);
    }

}
