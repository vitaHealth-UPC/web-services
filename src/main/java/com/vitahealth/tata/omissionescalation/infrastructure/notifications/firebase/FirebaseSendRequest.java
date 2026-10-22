package com.vitahealth.tata.omissionescalation.infrastructure.notifications.firebase;

/** HTTP v1 request envelope; no validation-only request is reported as an accepted delivery. */
public record FirebaseSendRequest(FirebaseMessage message) {
  /** Suppresses the message contents in diagnostic representations. */
  @Override
  public String toString() {
    return "FirebaseSendRequest[redacted]";
  }
}
