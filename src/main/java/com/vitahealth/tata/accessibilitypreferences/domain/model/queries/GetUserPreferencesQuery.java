package com.vitahealth.tata.accessibilitypreferences.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Selects saved preferences or non-persisted product defaults for one logical user. */
public record GetUserPreferencesQuery(String userId) {
  /** Rejects absent owners before persistence access and normalizes the lookup reference. */
  public GetUserPreferencesQuery {
    userId = requireText(userId, "userId");
  }
}
