package com.vitahealth.tata.familymonitoring.application.result;

import com.vitahealth.tata.familymonitoring.domain.model.entities.LowStockNotice;
import java.time.Instant;

/** Immutable source stock evidence captured for a caregiver status response. */
public record LowStockNoticeResult(
    String medicationId,
    String medicationName,
    int remainingStock,
    int replenishmentThreshold,
    Instant detectedAt) {
  /** Copies original evidence without retaining a mutable domain entity. */
  public static LowStockNoticeResult from(LowStockNotice source) {
    return new LowStockNoticeResult(
        source.getMedicationId(),
        source.getMedicationName(),
        source.getRemainingStock(),
        source.getReplenishmentThreshold(),
        source.getDetectedAt());
  }
}
