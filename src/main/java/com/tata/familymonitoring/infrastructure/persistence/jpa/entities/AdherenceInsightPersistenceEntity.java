package com.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "adherence_insights")
public class AdherenceInsightPersistenceEntity {


  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 36)
  private String medicationId;

  @Column(nullable = false)
  private String medicationName;

  @Column(nullable = false)
  private int omissionDays;

  @Column(nullable = false)
  private LocalDate firstDay;

  @Column(nullable = false)
  private LocalDate lastDay;

  @Column(nullable = false)
  private Instant detectedAt;


  public AdherenceInsightPersistenceEntity() {}
  public Long getId() { return id; }
  public void setId(Long value) { this.id = value; }
  public String getMedicationId() { return medicationId; }
  public void setMedicationId(String value) { this.medicationId = value; }
  public String getMedicationName() { return medicationName; }
  public void setMedicationName(String value) { this.medicationName = value; }
  public int getOmissionDays() { return omissionDays; }
  public void setOmissionDays(int value) { this.omissionDays = value; }
  public LocalDate getFirstDay() { return firstDay; }
  public void setFirstDay(LocalDate value) { this.firstDay = value; }
  public LocalDate getLastDay() { return lastDay; }
  public void setLastDay(LocalDate value) { this.lastDay = value; }
  public Instant getDetectedAt() { return detectedAt; }
  public void setDetectedAt(Instant value) { this.detectedAt = value; }
}
