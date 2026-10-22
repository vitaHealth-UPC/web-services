package com.vitahealth.tata.carelink.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Looks up relationship evidence when absence is an expected business outcome. */
public record FindCareLinkByIdQuery(String careLinkId) {
  /** Requires a nonblank logical identity without transport or persistence dependencies. */
  public FindCareLinkByIdQuery {
    careLinkId = requireText(careLinkId, "careLinkId");
  }
}
