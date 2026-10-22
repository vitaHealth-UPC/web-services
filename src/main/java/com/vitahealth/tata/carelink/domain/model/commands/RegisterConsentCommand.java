package com.vitahealth.tata.carelink.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Carries the adult decision for the relationship authorized by the request boundary. */
public record RegisterConsentCommand(String careLinkId, boolean accepted) {
  /** Requires normalized logical identities independently of transport and persistence. */
  public RegisterConsentCommand {
    careLinkId = requireText(careLinkId, "careLinkId");
  }
}
