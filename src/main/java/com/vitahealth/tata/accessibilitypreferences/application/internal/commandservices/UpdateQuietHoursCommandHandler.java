package com.vitahealth.tata.accessibilitypreferences.application.internal.commandservices;

import com.vitahealth.tata.accessibilitypreferences.application.PreferencesApplicationException;
import com.vitahealth.tata.accessibilitypreferences.application.internal.assemblers.UserPreferencesResultFromAggregateAssembler;
import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateQuietHoursCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies a single preference change while holding the logical user reservation. */
@Service
@Transactional
public class UpdateQuietHoursCommandHandler {
  private final UserPreferencesRepository repository;
  private final UserPreferencesMutationService mutations;

  /** Injects persistence and the shared reserved-state loader. */
  public UpdateQuietHoursCommandHandler(
      UserPreferencesRepository repository, UserPreferencesMutationService mutations) {
    this.repository = repository;
    this.mutations = mutations;
  }

  /** Changes only the selected setting and saves the complete reserved state. */
  public UserPreferencesResult handle(UpdateQuietHoursCommand command) {
    var preferences = mutations.load(command.userId());
    try {
      preferences.setQuietHours(command.quietHours());
    } catch (IllegalArgumentException rejectedDecision) {
      throw new PreferencesApplicationException();
    }
    return UserPreferencesResultFromAggregateAssembler.toResult(repository.save(preferences));
  }
}
