package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Replaces the reduced-motion choice for one logical user. */
public record UpdateReducedMotionCommand(String userId, boolean enabled) {
  /** Requires the intended owner before the decision can reach persistence. */
  public UpdateReducedMotionCommand {
    userId = requireText(userId, "userId");
  }
}
