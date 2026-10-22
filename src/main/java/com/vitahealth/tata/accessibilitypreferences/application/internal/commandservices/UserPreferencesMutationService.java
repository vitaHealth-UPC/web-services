package com.vitahealth.tata.accessibilitypreferences.application.internal.commandservices;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.accessibilitypreferences.application.internal.outboundservices.PreferencesMutationLock;
import com.vitahealth.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.vitahealth.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import java.time.Clock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserves the owner before loading mutable state and distinguishes loading from initialization.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class UserPreferencesMutationService {
  private final UserPreferencesRepository repository;
  private final PreferencesMutationLock reservations;
  private final Clock clock;

  /** Injects fresh-state persistence, user coordination and explicit application time. */
  public UserPreferencesMutationService(
      UserPreferencesRepository repository, PreferencesMutationLock reservations, Clock clock) {
    this.repository = repository;
    this.reservations = reservations;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Returns mutable state without a save; the command writes its fully validated decision once. */
  public UserPreferences load(String userId) {
    var owner = requireText(userId, "userId");
    reservations.reserve(owner);
    return repository
        .findByUserIdForUpdate(owner)
        .orElseGet(() -> UserPreferencesFactory.createDefaults(owner, clock.instant()));
  }

  /**
   * Deliberately creates absent preferences, preserving an existing record without another save.
   */
  public UserPreferences initialize(String userId) {
    var owner = requireText(userId, "userId");
    reservations.reserve(owner);
    return repository
        .findByUserIdForUpdate(owner)
        .orElseGet(
            () -> repository.save(UserPreferencesFactory.createDefaults(owner, clock.instant())));
  }
}
