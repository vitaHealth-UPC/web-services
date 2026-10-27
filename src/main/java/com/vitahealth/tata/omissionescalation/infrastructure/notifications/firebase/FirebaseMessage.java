package com.vitahealth.tata.omissionescalation.infrastructure.notifications.firebase;

import java.util.Map;

/** FCM data message targeting a device token resolved by the authorized recipient registry. */
public record FirebaseMessage(
    String token, Map<String, String> data, AndroidDelivery android, AppleDelivery apns) {
  public FirebaseMessage(String token, Map<String, String> data) {
    this(
        token,
        data,
        new AndroidDelivery("HIGH"),
        new AppleDelivery(
            Map.of("apns-priority", "5", "apns-push-type", "background"),
            Map.of("aps", Map.of("content-available", 1))));
  }

  public record AndroidDelivery(String priority) {}

  public record AppleDelivery(Map<String, String> headers, Map<String, Object> payload) {}

  /** Copies the payload so concurrent caller mutations cannot change a submitted delivery. */
  public FirebaseMessage {
    if (token == null || token.isBlank())
      throw new IllegalArgumentException("device token is required");
    if (data == null || data.isEmpty())
      throw new IllegalArgumentException("message data is required");
    data = Map.copyOf(data);
  }

  /** Suppresses device identifiers and clinical payloads in diagnostic representations. */
  @Override
  public String toString() {
    return "FirebaseMessage[redacted]";
  }
}
