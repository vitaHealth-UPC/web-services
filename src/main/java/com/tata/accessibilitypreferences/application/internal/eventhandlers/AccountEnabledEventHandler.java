package com.tata.accessibilitypreferences.application.internal.eventhandlers;

import com.tata.accessibilitypreferences.application.internal.commandservices.InitializeDefaultPreferencesCommandHandler;
import com.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import com.tata.identitysubscription.domain.model.events.AccountEnabled;
import org.springframework.stereotype.Service;

@Service
public class AccountEnabledEventHandler {
    private final InitializeDefaultPreferencesCommandHandler initializer;

    public AccountEnabledEventHandler(InitializeDefaultPreferencesCommandHandler initializer) {
        this.initializer = initializer;
    }

    public void handle(AccountEnabled event) {
        initializer.handle(new InitializeDefaultPreferencesCommand(event.accountId()));
    }
}
