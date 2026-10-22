package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Replaces the voice-confirmation choice for one logical user. */
public record UpdateVoiceConfirmationCommand(String userId, boolean enabled) {
  /** Requires the intended owner before the decision can reach persistence. */
  public UpdateVoiceConfirmationCommand {
    userId = requireText(userId, "userId");
  }
}
