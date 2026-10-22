package com.vitahealth.tata.accessibilitypreferences.domain.repositories;

import com.vitahealth.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import java.util.Optional;

/** Persists preference aggregates independently of identity accounts and transport resources. */
public interface UserPreferencesRepository {
  /** Saves a complete preference decision; application writers first reserve its logical owner. */
  UserPreferences save(UserPreferences preferences);

  /** Reads saved state without creating defaults or acquiring a mutation reservation. */
  Optional<UserPreferences> findByUserId(String userId);

  /** Locks and refreshes existing state within the caller's reserved mutation transaction. */
  Optional<UserPreferences> findByUserIdForUpdate(String userId);
}
