package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Replaces the dark-theme choice for one logical user. */
public record UpdateDarkThemeCommand(String userId, boolean enabled) {
  /** Requires the intended owner before the decision can reach persistence. */
  public UpdateDarkThemeCommand {
    userId = requireText(userId, "userId");
  }
}
