package com.vitahealth.tata.carelink.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Requests withdrawal by the authenticated adult, never by an actor supplied in a request body. */
public record WithdrawCareLinkConsentCommand(String careLinkId, String actingOlderAdultId) {
  /** Requires explicit relationship and adult identities for the domain ownership check. */
  public WithdrawCareLinkConsentCommand {
    careLinkId = requireText(careLinkId, "careLinkId");
    actingOlderAdultId = requireText(actingOlderAdultId, "actingOlderAdultId");
  }
}
