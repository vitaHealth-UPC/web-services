package com.tata.familymonitoring.domain.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

/** A medication of the older adult whose stock fell under its replenishment threshold. */
@Entity
public class LowStockNotice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 36)
  private String medicationId;

  @Column(nullable = false)
  private String medicationName;

  @Column(nullable = false)
  private int remainingStock;

  @Column(nullable = false)
  private int replenishmentThreshold;

  @Column(nullable = false)
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
}
