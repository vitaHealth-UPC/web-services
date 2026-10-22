package com.vitahealth.tata.carelink.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Scans confirmed caregiver recipients for one adult without exposing linking credentials. */
public record GetAuthorizedCaregiversByOlderAdultQuery(
    String olderAdultId, String afterId, int limit) {
  /** Requires a bounded explicit cursor; an empty cursor starts the scan. */
  public GetAuthorizedCaregiversByOlderAdultQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    if (afterId == null || limit < 1 || limit > 100)
      throw new IllegalArgumentException("caregiver recipient page is invalid");
  }
}
