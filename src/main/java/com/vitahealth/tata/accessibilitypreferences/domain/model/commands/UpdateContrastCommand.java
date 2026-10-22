package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Replaces the high-contrast choice for one logical user. */
public record UpdateContrastCommand(String userId, boolean enabled) {
  /** Requires the intended owner before the decision can reach persistence. */
  public UpdateContrastCommand {
    userId = requireText(userId, "userId");
  }
}
