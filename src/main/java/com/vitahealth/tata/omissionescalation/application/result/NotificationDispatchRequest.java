package com.vitahealth.tata.omissionescalation.application.result;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import java.time.Instant;

/** Immutable authorized attempt payload; token resolution happens immediately before transport. */
public record NotificationDispatchRequest(
    String deliveryId,
    String intakeId,
    String olderAdultId,
    NotificationKind kind,
    String medicationName,
    Instant scheduledAt,
    String deviceToken) {
  /** Requires exact source evidence without inventing schedule times or clinical dosage. */
  public NotificationDispatchRequest {
    if (deliveryId == null
        || deliveryId.isBlank()
        || intakeId == null
        || intakeId.isBlank()
        || olderAdultId == null
        || olderAdultId.isBlank()
        || kind == null
        || medicationName == null
        || medicationName.isBlank()
        || scheduledAt == null
        || deviceToken == null
        || deviceToken.isBlank())
      throw new IllegalArgumentException("notification dispatch evidence is required");
  }

  /**
   * Keeps the current device address and clinical payload out of automatic diagnostic formatting.
   */
  @Override
  public String toString() {
    return "NotificationDispatchRequest[redacted]";
  }
}
