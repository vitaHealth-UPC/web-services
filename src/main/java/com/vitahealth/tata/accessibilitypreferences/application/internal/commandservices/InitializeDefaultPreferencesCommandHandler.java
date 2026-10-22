package com.vitahealth.tata.accessibilitypreferences.application.internal.commandservices;

import com.vitahealth.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deliberately initializes preferences under the same owner reservation used by every mutation. */
@Service
@Transactional
public class InitializeDefaultPreferencesCommandHandler {
  private final UserPreferencesMutationService mutations;

  /** Injects reserved initialization without committing defaults independently of the caller. */
  public InitializeDefaultPreferencesCommandHandler(UserPreferencesMutationService mutations) {
    this.mutations = mutations;
  }

  /** Creates absent defaults once and preserves already saved choices without another write. */
  public UserPreferences handle(InitializeDefaultPreferencesCommand command) {
    return mutations.initialize(command.userId());
  }
}
