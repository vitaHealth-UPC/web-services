package com.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "low_stock_notices")
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


  public LowStockNoticePersistenceEntity() {}
  public Long getId() { return id; }
  public void setId(Long value) { this.id = value; }
  public String getMedicationId() { return medicationId; }
  public void setMedicationId(String value) { this.medicationId = value; }
  public String getMedicationName() { return medicationName; }
  public void setMedicationName(String value) { this.medicationName = value; }
  public int getRemainingStock() { return remainingStock; }
  public void setRemainingStock(int value) { this.remainingStock = value; }
  public int getReplenishmentThreshold() { return replenishmentThreshold; }
  public void setReplenishmentThreshold(int value) { this.replenishmentThreshold = value; }
  public Instant getDetectedAt() { return detectedAt; }
  public void setDetectedAt(Instant value) { this.detectedAt = value; }
}
