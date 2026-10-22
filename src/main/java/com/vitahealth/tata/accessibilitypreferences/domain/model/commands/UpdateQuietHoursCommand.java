package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;

/** Replaces the quiet interval; a null interval deliberately removes it. */
public record UpdateQuietHoursCommand(String userId, QuietHoursRange quietHours) {
  /** Requires the intended owner before the decision can reach persistence. */
  public UpdateQuietHoursCommand {
    userId = requireText(userId, "userId");
  }
}
