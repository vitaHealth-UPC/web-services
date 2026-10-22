package com.vitahealth.tata.carelink.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Requests a pending linking code for a profile registered by the caregiver. */
public record GenerateLinkingCodeCommand(String caregiverId, String olderAdultId) {
  /** Requires normalized logical identities independently of transport and persistence. */
  public GenerateLinkingCodeCommand {
    caregiverId = requireText(caregiverId, "caregiverId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
  }
}
