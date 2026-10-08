package com.tata.accessibilitypreferences.application.internal.commandservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateQuietHoursCommand;
import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateQuietHoursCommandHandler implements com.tata.accessibilitypreferences.application.commandservices.UpdateQuietHoursCommandService {
    private final IUserPreferencesRepository repository;
    private final InitializeDefaultPreferencesCommandHandler initializer;

    public UpdateQuietHoursCommandHandler(
            IUserPreferencesRepository repository, InitializeDefaultPreferencesCommandHandler initializer) {
        this.repository = repository;
        this.initializer = initializer;
    }

    public UserPreferences handle(UpdateQuietHoursCommand command) {
        var preferences = initializer.handle(new InitializeDefaultPreferencesCommand(command.userId()));
        preferences.setQuietHours(command.quietHours());
        return repository.save(preferences);
    }
}
