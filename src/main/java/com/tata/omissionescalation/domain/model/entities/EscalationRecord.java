package com.tata.omissionescalation.domain.model.entities;

import com.tata.omissionescalation.domain.model.valueobjects.EscalationLevel;
import java.time.Instant;

/** One increment of the attention level applied to an omission case. */

public class EscalationRecord {


  private Long id;

  private EscalationLevel level;

  private String reason;

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

  /** Restores persisted state without replaying business actions. */
  public static EscalationRecord rehydrate(Long id, EscalationLevel level, String reason, Instant triggeredAt) {
    var restored = new EscalationRecord();
    restored.id = id;
    restored.level = level;
    restored.reason = reason;
    restored.triggeredAt = triggeredAt;
    return restored;
  }
}
