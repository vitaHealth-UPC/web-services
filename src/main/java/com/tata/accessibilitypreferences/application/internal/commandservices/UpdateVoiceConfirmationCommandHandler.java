package com.tata.accessibilitypreferences.application.internal.commandservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateVoiceConfirmationCommand;
import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateVoiceConfirmationCommandHandler implements com.tata.accessibilitypreferences.application.commandservices.UpdateVoiceConfirmationCommandService {
    private final IUserPreferencesRepository repository;
    private final InitializeDefaultPreferencesCommandHandler initializer;

    public UpdateVoiceConfirmationCommandHandler(
            IUserPreferencesRepository repository, InitializeDefaultPreferencesCommandHandler initializer) {
        this.repository = repository;
        this.initializer = initializer;
    }

    public UserPreferences handle(UpdateVoiceConfirmationCommand command) {
        var preferences = initializer.handle(new InitializeDefaultPreferencesCommand(command.userId()));
        preferences.setVoiceConfirmation(command.enabled());
        return repository.save(preferences);
    }
}
