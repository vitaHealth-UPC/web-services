package com.tata.accessibilitypreferences.application.internal.commandservices;

import com.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class InitializeDefaultPreferencesCommandHandler {
    private final IUserPreferencesRepository repository;

    public InitializeDefaultPreferencesCommandHandler(IUserPreferencesRepository repository) {
        this.repository = repository;
    }

    /** Creates the default preferences once; if the user already has them they are returned unchanged. */
    public UserPreferences handle(InitializeDefaultPreferencesCommand command) {
        return repository.findByUserId(command.userId())
                .orElseGet(() -> repository.save(UserPreferencesFactory.createDefaults(command.userId(), Instant.now())));
    }
}
