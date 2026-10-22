package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Creates absent product preferences without replacing existing user choices. */
public record InitializeDefaultPreferencesCommand(String userId) {
  /** Requires the intended owner before the decision can reach persistence. */
  public InitializeDefaultPreferencesCommand {
    userId = requireText(userId, "userId");
  }
}
