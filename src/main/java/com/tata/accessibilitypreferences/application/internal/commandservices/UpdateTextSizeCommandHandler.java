package com.tata.accessibilitypreferences.application.internal.commandservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateTextSizeCommand;
import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateTextSizeCommandHandler {
    private final IUserPreferencesRepository repository;
    private final InitializeDefaultPreferencesCommandHandler initializer;

    public UpdateTextSizeCommandHandler(
            IUserPreferencesRepository repository, InitializeDefaultPreferencesCommandHandler initializer) {
        this.repository = repository;
        this.initializer = initializer;
    }

    public UserPreferences handle(UpdateTextSizeCommand command) {
        var preferences = initializer.handle(new InitializeDefaultPreferencesCommand(command.userId()));
        preferences.updateTextSize(command.textSize());
        return repository.save(preferences);
    }
}
