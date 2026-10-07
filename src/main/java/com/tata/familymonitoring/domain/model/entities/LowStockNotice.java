package com.tata.familymonitoring.domain.model.entities;

import java.time.Instant;

/** A medication of the older adult whose stock fell under its replenishment threshold. */

public class LowStockNotice {


  private Long id;

  private String medicationId;

  private String medicationName;

  private int remainingStock;

  private int replenishmentThreshold;

  private Instant detectedAt;

  protected LowStockNotice() {
  }

  public LowStockNotice(
      String medicationId, String medicationName, int remainingStock, int replenishmentThreshold, Instant detectedAt) {
    this.medicationId = medicationId;
    this.medicationName = medicationName;
    this.remainingStock = remainingStock;
    this.replenishmentThreshold = replenishmentThreshold;
    this.detectedAt = detectedAt;
  }

  public void refresh(String medicationName, int remainingStock, int replenishmentThreshold, Instant detectedAt) {
    this.medicationName = medicationName;
    this.remainingStock = remainingStock;
    this.replenishmentThreshold = replenishmentThreshold;
    this.detectedAt = detectedAt;
  }

  public Long getId() {
    return id;
  }

  public String getMedicationId() {
    return medicationId;
  }

  public String getMedicationName() {
    return medicationName;
  }

  public int getRemainingStock() {
    return remainingStock;
  }

  public int getReplenishmentThreshold() {
    return replenishmentThreshold;
  }

  public Instant getDetectedAt() {
    return detectedAt;
  }

  /** Restores persisted state without replaying business actions. */
  public static LowStockNotice rehydrate(Long id, String medicationId, String medicationName, int remainingStock, int replenishmentThreshold, Instant detectedAt) {
    var restored = new LowStockNotice();
    restored.id = id;
    restored.medicationId = medicationId;
    restored.medicationName = medicationName;
    restored.remainingStock = remainingStock;
    restored.replenishmentThreshold = replenishmentThreshold;
    restored.detectedAt = detectedAt;
    return restored;
  }
}
