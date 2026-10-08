package com.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "personal_notes",
    indexes =
        @Index(name = "idx_personal_notes_owner_date", columnList = "older_adult_id,recorded_at"))
public class PersonalNotePersistenceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "older_adult_id", nullable = false, length = 36)
  private String olderAdultId;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, length = 1000)
  private String text;

  @Column(nullable = false, length = 20)
  private String category;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  public PersonalNotePersistenceEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long v) {
    id = v;
  }

  public String getOlderAdultId() {
    return olderAdultId;
  }

  public void setOlderAdultId(String v) {
    olderAdultId = v;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String v) {
    title = v;
  }

  public String getText() {
    return text;
  }

  public void setText(String v) {
    text = v;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String v) {
    category = v;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  public void setRecordedAt(Instant v) {
    recordedAt = v;
  }
}
