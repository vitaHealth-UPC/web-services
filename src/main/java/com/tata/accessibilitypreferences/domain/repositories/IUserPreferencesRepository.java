package com.tata.accessibilitypreferences.domain.repositories;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import java.util.Optional;

public interface IUserPreferencesRepository {
    UserPreferences save(UserPreferences preferences);
    Optional<UserPreferences> findByUserId(String userId);
}
