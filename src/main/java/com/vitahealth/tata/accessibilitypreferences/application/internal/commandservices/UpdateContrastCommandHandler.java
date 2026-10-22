package com.vitahealth.tata.accessibilitypreferences.application.internal.commandservices;

import com.vitahealth.tata.accessibilitypreferences.application.PreferencesApplicationException;
import com.vitahealth.tata.accessibilitypreferences.application.internal.assemblers.UserPreferencesResultFromAggregateAssembler;
import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateContrastCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies a single preference change while holding the logical user reservation. */
@Service
@Transactional
public class UpdateContrastCommandHandler {
  private final UserPreferencesRepository repository;
  private final UserPreferencesMutationService mutations;

  /** Injects persistence and the shared reserved-state loader. */
  public UpdateContrastCommandHandler(
      UserPreferencesRepository repository, UserPreferencesMutationService mutations) {
    this.repository = repository;
    this.mutations = mutations;
  }

  /** Changes only the selected setting and saves the complete reserved state. */
  public UserPreferencesResult handle(UpdateContrastCommand command) {
    var preferences = mutations.load(command.userId());
    try {
      preferences.setHighContrast(command.enabled());
    } catch (IllegalArgumentException rejectedDecision) {
      throw new PreferencesApplicationException();
    }
    return UserPreferencesResultFromAggregateAssembler.toResult(repository.save(preferences));
  }
}
