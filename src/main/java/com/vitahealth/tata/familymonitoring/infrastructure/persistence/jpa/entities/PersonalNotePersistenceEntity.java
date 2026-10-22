package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** Stores immutable private adult reminder evidence and its original recording time. */
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

  /** Required by the persistence provider. */
  public PersonalNotePersistenceEntity() {}

  /** Returns stored id. */
  public Long getId() {
    return id;
  }

  /** Copies id during persistence assembly. */
  public void setId(Long value) {
    id = value;
  }

  /** Returns stored olderAdultId. */
  public String getOlderAdultId() {
    return olderAdultId;
  }

  /** Copies olderAdultId during persistence assembly. */
  public void setOlderAdultId(String value) {
    olderAdultId = value;
  }

  /** Returns stored title. */
  public String getTitle() {
    return title;
  }

  /** Copies title during persistence assembly. */
  public void setTitle(String value) {
    title = value;
  }

  /** Returns stored text. */
  public String getText() {
    return text;
  }

  /** Copies text during persistence assembly. */
  public void setText(String value) {
    text = value;
  }

  /** Returns stored category. */
  public String getCategory() {
    return category;
  }

  /** Copies category during persistence assembly. */
  public void setCategory(String value) {
    category = value;
  }

  /** Returns stored recordedAt. */
  public Instant getRecordedAt() {
    return recordedAt;
  }

  /** Copies recordedAt during persistence assembly. */
  public void setRecordedAt(Instant value) {
    recordedAt = value;
  }
}
