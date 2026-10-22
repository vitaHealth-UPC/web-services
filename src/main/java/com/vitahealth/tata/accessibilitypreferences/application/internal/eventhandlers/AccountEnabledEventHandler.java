package com.vitahealth.tata.accessibilitypreferences.application.internal.eventhandlers;

import com.vitahealth.tata.accessibilitypreferences.application.internal.commandservices.InitializeDefaultPreferencesCommandHandler;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.InitializeDefaultPreferencesCommand;
import com.vitahealth.tata.identitysubscription.domain.model.events.AccountEnabled;
import org.springframework.stereotype.Service;

/** Translates account activation into idempotent preference initialization. */
@Service
public class AccountEnabledEventHandler {
  private final InitializeDefaultPreferencesCommandHandler initializer;

  /** Injects the initializer that reserves the owner and preserves existing preferences. */
  public AccountEnabledEventHandler(InitializeDefaultPreferencesCommandHandler initializer) {
    this.initializer = initializer;
  }

  /** Initializes the event account without replacing choices already saved by that owner. */
  public void handle(AccountEnabled event) {
    initializer.handle(new InitializeDefaultPreferencesCommand(event.accountId()));
  }
}
