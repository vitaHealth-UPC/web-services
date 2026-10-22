package com.vitahealth.tata.omissionescalation.application.result;

/** Omission-owned dispatch address carrying the installation and its revocable session scope. */
public record NotificationDeviceAddress(String installationId, String sessionId, String token) {
  /** Hides opaque delivery addresses and session references in diagnostics. */
  @Override
  public String toString() {
    return "NotificationDeviceAddress[redacted]";
  }
}
