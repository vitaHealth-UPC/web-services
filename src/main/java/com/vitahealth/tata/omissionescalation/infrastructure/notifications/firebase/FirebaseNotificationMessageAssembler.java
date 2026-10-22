package com.vitahealth.tata.omissionescalation.infrastructure.notifications.firebase;

import com.vitahealth.tata.omissionescalation.application.result.NotificationDispatchRequest;
import java.util.Map;
import java.util.Objects;

/**
 * Maps authorized delivery evidence into a versioned, language-independent Android data contract.
 */
public final class FirebaseNotificationMessageAssembler {
  private FirebaseNotificationMessageAssembler() {}

  /**
   * Preserves the stable delivery identity so retries can be suppressed by the receiving device.
   */
  public static FirebaseMessage toMessage(NotificationDispatchRequest request) {
    Objects.requireNonNull(request, "request");
    return new FirebaseMessage(
        request.deviceToken(),
        Map.of(
            "schemaVersion", "1",
            "deliveryId", request.deliveryId(),
            "intakeId", request.intakeId(),
            "olderAdultId", request.olderAdultId(),
            "kind", request.kind().name(),
            "medicationName", request.medicationName(),
            "scheduledAt", request.scheduledAt().toString()));
  }
}
