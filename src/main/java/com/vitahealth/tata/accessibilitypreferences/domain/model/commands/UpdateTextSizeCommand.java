package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;

/** Replaces the text-size level for one logical user. */
public record UpdateTextSizeCommand(String userId, TextSizeLevel textSize) {
  /** Requires the intended owner before the decision can reach persistence. */
  public UpdateTextSizeCommand {
    userId = requireText(userId, "userId");
    if (textSize == null) throw new IllegalArgumentException("textSize is required");
  }
}
