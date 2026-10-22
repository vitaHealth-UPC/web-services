package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.embeddables.EscalationLevelPersistenceEmbeddable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Stores one immutable escalation occurrence owned by its omission case. */
@Entity
@Table(name = "escalation_records")
public class EscalationRecordPersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Embedded private EscalationLevelPersistenceEmbeddable level;

  @Column(nullable = false)
  private String reason;

  @Column(nullable = false)
  private Instant triggeredAt;

  /** Supports JPA hydration and the context persistence assembler. */
  public EscalationRecordPersistenceEntity() {}

  /** Returns the stored generated identity. */
  public Long getId() {
    return id;
  }

  /**
   * Assigns the stored generated identity during snapshot assembly; managed updates preserve
   * existing evidence.
   */
  public void setId(Long value) {
    this.id = value;
  }

  /** Returns the stored positive attention level. */
  public EscalationLevelPersistenceEmbeddable getLevel() {
    return level;
  }

  /** Assigns the stored positive attention level. */
  public void setLevel(EscalationLevelPersistenceEmbeddable value) {
    this.level = value;
  }

  /** Returns the original escalation reason. */
  public String getReason() {
    return reason;
  }

  /** Assigns the original escalation reason. */
  public void setReason(String value) {
    this.reason = value;
  }

  /** Returns the original escalation occurrence instant. */
  public Instant getTriggeredAt() {
    return triggeredAt;
  }

  /** Assigns the original escalation occurrence instant. */
  public void setTriggeredAt(Instant value) {
    this.triggeredAt = value;
  }
}
