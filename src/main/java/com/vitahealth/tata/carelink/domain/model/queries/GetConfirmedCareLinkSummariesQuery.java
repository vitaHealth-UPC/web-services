package com.vitahealth.tata.carelink.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Requests credential-free display projections for one caregiver's confirmed relationships. */
public record GetConfirmedCareLinkSummariesQuery(String caregiverId) {
  /** Requires an explicit caregiver identity independently of its HTTP representation. */
  public GetConfirmedCareLinkSummariesQuery {
    caregiverId = requireText(caregiverId, "caregiverId");
  }
}
