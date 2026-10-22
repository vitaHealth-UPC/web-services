package com.vitahealth.tata.carelink.domain.model.events;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/**
 * Announces confirmation inside the write transaction to consumers sharing its rollback boundary.
 */
public record CareLinkConfirmed(String careLinkId, String caregiverId, String olderAdultId) {
  /** Requires normalized logical identities independently of transport and persistence. */
  public CareLinkConfirmed {
    careLinkId = requireText(careLinkId, "careLinkId");
    caregiverId = requireText(caregiverId, "caregiverId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
  }
}
