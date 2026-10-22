package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** Stores unresolved source inventory evidence owned by one relationship follow-up. */
@Entity
@Table(
    name = "low_stock_notices",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_follow_up_stock_medication",
            columnNames = {"family_monitor_id", "medication_id"}))
public class LowStockNoticePersistenceEntity {

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

  /** Required by the persistence provider. */
  public LowStockNoticePersistenceEntity() {}

  /** Returns stored row identity. */
  public Long getId() {
    return id;
  }

  /** Copies stored row identity during managed persistence assembly or update. */
  public void setId(Long value) {
    this.id = value;
  }

  /** Returns source medication reference. */
  public String getMedicationId() {
    return medicationId;
  }

  /** Copies source medication reference during managed persistence assembly or update. */
  public void setMedicationId(String value) {
    this.medicationId = value;
  }

  /** Returns medication name recorded in source evidence. */
  public String getMedicationName() {
    return medicationName;
  }

  /**
   * Copies medication name recorded in source evidence during managed persistence assembly or
   * update.
   */
  public void setMedicationName(String value) {
    this.medicationName = value;
  }

  /** Returns remaining inventory units reported by the source. */
  public int getRemainingStock() {
    return remainingStock;
  }

  /**
   * Copies remaining inventory units reported by the source during managed persistence assembly or
   * update.
   */
  public void setRemainingStock(int value) {
    this.remainingStock = value;
  }

  /** Returns threshold accompanying source shortage evidence. */
  public int getReplenishmentThreshold() {
    return replenishmentThreshold;
  }

  /**
   * Copies threshold accompanying source shortage evidence during managed persistence assembly or
   * update.
   */
  public void setReplenishmentThreshold(int value) {
    this.replenishmentThreshold = value;
  }

  /** Returns original source detection instant. */
  public Instant getDetectedAt() {
    return detectedAt;
  }

  /** Copies original source detection instant during managed persistence assembly or update. */
  public void setDetectedAt(Instant value) {
    this.detectedAt = value;
  }
}
