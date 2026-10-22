package com.vitahealth.tata.carelink.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;

/** Requests confirmation at the occurrence time observed by the consent application workflow. */
public record ConfirmCareLinkCommand(String careLinkId, Instant confirmedAt) {
  /** Requires relationship identity and the server-observed occurrence time. */
  public ConfirmCareLinkCommand {
    careLinkId = requireText(careLinkId, "careLinkId");
    Objects.requireNonNull(confirmedAt, "confirmedAt");
  }
}
