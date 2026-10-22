package com.vitahealth.tata.carelink.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Requests consumption of the caregiver-owned linking code without granting consent. */
public record AcceptCareLinkCommand(String caregiverId, String code) {
  /** Requires normalized logical identities independently of transport and persistence. */
  public AcceptCareLinkCommand {
    caregiverId = requireText(caregiverId, "caregiverId");
  }

  /** Keeps the linking credential out of diagnostic output. */
  @Override
  public String toString() {
    return "AcceptCareLinkCommand[caregiverId=" + caregiverId + ", code=REDACTED]";
  }
}
