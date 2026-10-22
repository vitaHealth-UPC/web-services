package com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import java.time.Instant;
import java.util.Objects;

/** Immutable pending decrement evidence and its independently persisted retry position. */
public record PendingInventoryConsumption(
    String intakeId,
    String medicationId,
    String olderAdultId,
    Instant confirmedAt,
    Instant createdAt,
    Instant nextAttemptAt,
    int attempts) {
  /** Rejects impossible retry state while retaining the source confirmation unchanged. */
  public PendingInventoryConsumption {
    intakeId = requireText(intakeId, "intakeId");
    medicationId = requireText(medicationId, "medicationId");
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    Objects.requireNonNull(confirmedAt, "confirmedAt");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
    if (createdAt.isBefore(confirmedAt) || nextAttemptAt.isBefore(createdAt) || attempts < 0)
      throw new IllegalArgumentException("invalid inventory consumption retry state");
  }

  /** Starts an immediately eligible request in the source confirmation transaction. */
  public static PendingInventoryConsumption request(
      RequestInventoryConsumptionCommand command, Instant now) {
    Objects.requireNonNull(command, "command");
    return new PendingInventoryConsumption(
        command.intakeId(),
        command.medicationId(),
        command.olderAdultId(),
        command.confirmedAt(),
        now,
        now,
        0);
  }

  /** Checks duplicate delivery against every original source field, including its exact instant. */
  public boolean matches(RequestInventoryConsumptionCommand command) {
    Objects.requireNonNull(command, "command");
    return intakeId.equals(command.intakeId())
        && medicationId.equals(command.medicationId())
        && olderAdultId.equals(command.olderAdultId())
        && confirmedAt.equals(command.confirmedAt());
  }

  /**
   * Advances retry metadata without rewriting source evidence or mutating the previous snapshot.
   */
  public PendingInventoryConsumption retryAt(Instant next) {
    Objects.requireNonNull(next, "next");
    if (next.isBefore(nextAttemptAt))
      throw new IllegalArgumentException("retry position must not move backwards");
    return new PendingInventoryConsumption(
        intakeId,
        medicationId,
        olderAdultId,
        confirmedAt,
        createdAt,
        next,
        Math.incrementExact(attempts));
  }
}
