package com.vitahealth.tata.omissionescalation.domain.model.entities;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.EscalationLevel;
import java.time.Instant;

/** One increment of the attention level applied to an omission case. */
public class EscalationRecord {

  private Long id;

  private EscalationLevel level;

  private String reason;

  private Instant triggeredAt;

  private EscalationRecord() {}

  /** Creates one positive escalation level with explicit reason and occurrence time. */
  public EscalationRecord(EscalationLevel level, String reason, Instant triggeredAt) {
    validate(level, reason, triggeredAt);
    this.level = level;
    this.reason = reason;
    this.triggeredAt = triggeredAt;
  }

  private static void validate(EscalationLevel level, String reason, Instant triggeredAt) {
    if (level == null
        || level.value() == 0
        || reason == null
        || reason.isBlank()
        || triggeredAt == null)
      throw new IllegalArgumentException(
          "escalation requires a positive level, reason and occurrence time");
  }

  /** Returns the stored record identity, absent before its first save. */
  public Long getId() {
    return id;
  }

  /** Returns the recorded positive attention level. */
  public EscalationLevel getLevel() {
    return level;
  }

  /** Returns the original escalation reason. */
  public String getReason() {
    return reason;
  }

  /** Returns the explicit occurrence time. */
  public Instant getTriggeredAt() {
    return triggeredAt;
  }

  /** Restores persisted state without replaying business actions. */
  public static EscalationRecord rehydrate(
      Long id, EscalationLevel level, String reason, Instant triggeredAt) {
    validate(level, reason, triggeredAt);
    if (id == null || id < 1)
      throw new IllegalArgumentException("stored escalation identity is required");
    var restored = new EscalationRecord();
    restored.id = id;
    restored.level = level;
    restored.reason = reason;
    restored.triggeredAt = triggeredAt;
    return restored;
  }
}
