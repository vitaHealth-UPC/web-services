package com.vitahealth.tata.carelink.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Requests confirmed relationships for one caregiver. */
public record GetConfirmedCareLinksByCaregiverQuery(String caregiverId) {
  /** Requires explicit logical identities independently of transport and persistence. */
  public GetConfirmedCareLinksByCaregiverQuery {
    caregiverId = requireText(caregiverId, "caregiverId");
  }
}
