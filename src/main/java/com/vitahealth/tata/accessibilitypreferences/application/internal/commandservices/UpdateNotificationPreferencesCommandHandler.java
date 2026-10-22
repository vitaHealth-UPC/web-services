package com.vitahealth.tata.accessibilitypreferences.application.internal.commandservices;

import com.vitahealth.tata.accessibilitypreferences.application.PreferencesApplicationException;
import com.vitahealth.tata.accessibilitypreferences.application.internal.assemblers.UserPreferencesResultFromAggregateAssembler;
import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies the combined preference decision with one aggregate load and persistence write. */
@Service
public class UpdateNotificationPreferencesCommandHandler {
  private final UserPreferencesRepository repository;
  private final UserPreferencesMutationService mutations;

  /** Injects preference persistence and the shared reserved-state loader. */
  public UpdateNotificationPreferencesCommandHandler(
      UserPreferencesRepository repository, UserPreferencesMutationService mutations) {
    this.repository = repository;
    this.mutations = mutations;
  }

  /**
   * Preserves accessibility settings, notification timing and contacts while replacing both fields.
   */
  @Transactional
  public UserPreferencesResult handle(UpdateNotificationPreferencesCommand command) {
    var preferences = mutations.load(command.userId());
    try {
      preferences.updateNotificationPreferences(command.quietHours(), command.channels());
    } catch (IllegalArgumentException rejectedDecision) {
      throw new PreferencesApplicationException();
    }
    return UserPreferencesResultFromAggregateAssembler.toResult(repository.save(preferences));
  }
}
