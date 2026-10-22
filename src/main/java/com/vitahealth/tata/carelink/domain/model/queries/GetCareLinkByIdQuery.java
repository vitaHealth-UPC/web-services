package com.vitahealth.tata.carelink.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Requests current relationship evidence by its logical identity. */
public record GetCareLinkByIdQuery(String careLinkId) {
  /** Requires explicit logical identities independently of transport and persistence. */
  public GetCareLinkByIdQuery {
    careLinkId = requireText(careLinkId, "careLinkId");
  }
}
