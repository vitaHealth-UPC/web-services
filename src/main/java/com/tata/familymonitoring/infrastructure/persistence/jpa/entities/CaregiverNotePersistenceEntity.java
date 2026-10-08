package com.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "caregiver_notes")
public class CaregiverNotePersistenceEntity {


  public static final int MAX_LENGTH = 1000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 1000)
  private String text;

  @Column(nullable = false)
  private Instant recordedAt;

  @Column(nullable = false, length = 36)
  private String familiarId;


  public CaregiverNotePersistenceEntity() {}
  public Long getId() { return id; }
  public void setId(Long value) { this.id = value; }
  public String getText() { return text; }
  public void setText(String value) { this.text = value; }
  public Instant getRecordedAt() { return recordedAt; }
  public void setRecordedAt(Instant value) { this.recordedAt = value; }
  public String getFamiliarId() { return familiarId; }
  public void setFamiliarId(String value) { this.familiarId = value; }
}
