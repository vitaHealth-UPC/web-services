package com.vitahealth.tata.familymonitoring.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

/** Durable exclusive medication cursor for one relationship's initial inventory synchronization. */
public record FamilyStockCatchUp(
    long monitorId,
    String afterMedicationId,
    Instant requestedAt,
    Instant nextAttemptAt,
    int attempts) {
  /** Validates identity, cursor size and recovery chronology without changing stored evidence. */
  public FamilyStockCatchUp {
    if (monitorId <= 0) throw new IllegalArgumentException("monitor identity must be positive");
    Objects.requireNonNull(afterMedicationId, "afterMedicationId");
    Objects.requireNonNull(requestedAt, "requestedAt");
    Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
    if (afterMedicationId.length() > 36
        || (!afterMedicationId.isEmpty() && afterMedicationId.isBlank()))
      throw new IllegalArgumentException("invalid medication cursor");
    if (attempts < 0 || nextAttemptAt.isBefore(requestedAt))
      throw new IllegalArgumentException("invalid catch-up recovery evidence");
  }

  /** Starts before the first medication reference and is immediately eligible. */
  public static FamilyStockCatchUp start(long monitorId, Instant now) {
    return new FamilyStockCatchUp(monitorId, "", now, now, 0);
  }

  /** Advances strictly beyond the previous medication, retaining the original request instant. */
  public FamilyStockCatchUp advance(String medicationId, Instant now) {
    Objects.requireNonNull(medicationId, "medicationId");
    Objects.requireNonNull(now, "now");
    if (now.isBefore(nextAttemptAt)) throw new IllegalArgumentException("work is not due yet");
    if (medicationId.compareTo(afterMedicationId) <= 0)
      throw new IllegalArgumentException("medication cursor must advance");
    return new FamilyStockCatchUp(monitorId, medicationId, requestedAt, now, 0);
  }

  /** Schedules another attempt without discarding successfully processed medication references. */
  public FamilyStockCatchUp retryAt(Instant nextAttempt) {
    Objects.requireNonNull(nextAttempt, "nextAttempt");
    if (nextAttempt.isBefore(nextAttemptAt))
      throw new IllegalArgumentException("retry must not move backwards");
    return new FamilyStockCatchUp(
        monitorId, afterMedicationId, requestedAt, nextAttempt, Math.incrementExact(attempts));
  }
}
