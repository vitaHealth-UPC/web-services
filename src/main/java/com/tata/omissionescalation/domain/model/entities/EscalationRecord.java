package com.tata.omissionescalation.domain.model.entities;

import com.tata.omissionescalation.domain.model.valueobjects.EscalationLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

/** One increment of the attention level applied to an omission case. */
@Entity
public class EscalationRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Embedded
  private EscalationLevel level;

  @Column(nullable = false)
  private String reason;

  @Column(nullable = false)
  private Instant triggeredAt;

  protected EscalationRecord() {
  }

  public EscalationRecord(EscalationLevel level, String reason, Instant triggeredAt) {
    this.level = level;
    this.reason = reason;
    this.triggeredAt = triggeredAt;
  }

  public Long getId() {
    return id;
  }

  public EscalationLevel getLevel() {
    return level;
  }

  public String getReason() {
    return reason;
  }

  public Instant getTriggeredAt() {
    return triggeredAt;
  }
}
