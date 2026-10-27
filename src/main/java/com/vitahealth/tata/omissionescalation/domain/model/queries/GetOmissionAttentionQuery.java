package com.vitahealth.tata.omissionescalation.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.util.Set;

/** Immutable scope of an internal attention-level lookup. */
public record GetOmissionAttentionQuery(String olderAdultId, Set<String> intakeIds) {
  /** Captures the target set and rejects blank references before storage is queried. */
  public GetOmissionAttentionQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    intakeIds = Set.copyOf(intakeIds);
    intakeIds.forEach(id -> requireText(id, "intakeId"));
  }
}
