package com.vitahealth.tata.familymonitoring.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;

/** Projects original omission evidence into authorized relationship follow-up. */
public record RegisterAlertFromOmissionCommand(
    String olderAdultId,
    String intakeId,
    String medicationName,
    Instant scheduledAt,
    String reason,
    Instant occurredAt) {
  /** Requires original source evidence before selecting any relationship projections. */
  public RegisterAlertFromOmissionCommand {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    intakeId = requireText(intakeId, "intakeId");
    medicationName = requireText(medicationName, "medicationName");
    reason = requireText(reason, "reason");
    if (scheduledAt == null || occurredAt == null || occurredAt.isBefore(scheduledAt))
      throw new IllegalArgumentException("omission occurrence must not precede its schedule");
  }
}
