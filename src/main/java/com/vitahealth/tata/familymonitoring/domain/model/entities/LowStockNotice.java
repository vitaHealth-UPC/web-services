package com.vitahealth.tata.familymonitoring.domain.model.entities;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Instant;
import java.util.Objects;

/** A medication of the older adult whose stock fell under its replenishment threshold. */
public class LowStockNotice {

  private Long id;

  private final String medicationId;

  private String medicationName;

  private int remainingStock;

  private int replenishmentThreshold;

  private Instant detectedAt;

  /** Records original low-stock evidence with quantities expressed in inventory units. */
  public LowStockNotice(
      String medicationId,
      String medicationName,
      int remainingStock,
      int replenishmentThreshold,
      Instant detectedAt) {
    requireText(medicationId, "medicationId");
    validate(medicationName, remainingStock, replenishmentThreshold, detectedAt);
    this.medicationId = medicationId;
    this.medicationName = medicationName;
    this.remainingStock = remainingStock;
    this.replenishmentThreshold = replenishmentThreshold;
    this.detectedAt = detectedAt;
  }

  /** Applies non-older source evidence only after validating the entire replacement. */
  public void refresh(
      String medicationName, int remainingStock, int replenishmentThreshold, Instant detectedAt) {
    validate(medicationName, remainingStock, replenishmentThreshold, detectedAt);
    if (detectedAt.isBefore(this.detectedAt))
      throw new IllegalArgumentException("low-stock evidence must not move backwards");
    this.medicationName = medicationName;
    this.remainingStock = remainingStock;
    this.replenishmentThreshold = replenishmentThreshold;
    this.detectedAt = detectedAt;
  }

  /** Returns the stored identity, absent until first persistence. */
  public Long getId() {
    return id;
  }

  /** Returns the source medication reference. */
  public String getMedicationId() {
    return medicationId;
  }

  /** Returns the medication name recorded in the source evidence. */
  public String getMedicationName() {
    return medicationName;
  }

  /** Returns the remaining quantity in inventory units. */
  public int getRemainingStock() {
    return remainingStock;
  }

  /** Returns the threshold accompanying the low-stock evidence. */
  public int getReplenishmentThreshold() {
    return replenishmentThreshold;
  }

  /** Returns the source detection instant. */
  public Instant getDetectedAt() {
    return detectedAt;
  }

  /** Returns detached evidence while preserving stored identity and original detection time. */
  public LowStockNotice snapshot() {
    var copy =
        new LowStockNotice(
            medicationId, medicationName, remainingStock, replenishmentThreshold, detectedAt);
    copy.id = id;
    return copy;
  }

  /** Restores validated stored evidence, preserving original text and timestamps. */
  public static LowStockNotice rehydrate(
      Long id,
      String medicationId,
      String medicationName,
      int remainingStock,
      int replenishmentThreshold,
      Instant detectedAt) {
    if (id == null || id <= 0)
      throw new IllegalArgumentException("stored stock notice identity must be positive");
    var restored =
        new LowStockNotice(
            medicationId, medicationName, remainingStock, replenishmentThreshold, detectedAt);
    restored.id = id;
    return restored;
  }

  private static void validate(
      String medicationName, int remainingStock, int threshold, Instant detectedAt) {
    requireText(medicationName, "medicationName");
    Objects.requireNonNull(detectedAt, "detectedAt");
    if (remainingStock < 0 || threshold < 0 || remainingStock > threshold)
      throw new IllegalArgumentException(
          "low-stock quantities must be nonnegative and within threshold");
  }
}
