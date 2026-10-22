package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Replaces the reading-assistance choice for one logical user. */
public record UpdateReadingAssistanceCommand(String userId, boolean enabled) {
  /** Requires the intended owner before the decision can reach persistence. */
  public UpdateReadingAssistanceCommand {
    userId = requireText(userId, "userId");
  }
}
