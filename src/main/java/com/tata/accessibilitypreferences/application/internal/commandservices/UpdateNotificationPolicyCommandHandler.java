package com.tata.accessibilitypreferences.application.internal.commandservices;

import com.tata.accessibilitypreferences.application.commandservices.UpdateNotificationPolicyCommandService;
import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPolicyCommand;
import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saves a valid policy atomically with its recipients, preserving other user preferences. */
@Service
public class UpdateNotificationPolicyCommandHandler
    implements UpdateNotificationPolicyCommandService {
  private final IUserPreferencesRepository repository;
  private final InitializeDefaultPreferencesCommandHandler initializer;

  public UpdateNotificationPolicyCommandHandler(
      IUserPreferencesRepository repository,
      InitializeDefaultPreferencesCommandHandler initializer) {
    this.repository = repository;
    this.initializer = initializer;
  }

  @Override
  @Transactional
  public UserPreferences handle(UpdateNotificationPolicyCommand command) {
    var preferences = initializer.handle(new InitializeDefaultPreferencesCommand(command.userId()));
    preferences.updateNotificationPolicy(command.policy(), command.contacts());
    return repository.save(preferences);
  }
}
