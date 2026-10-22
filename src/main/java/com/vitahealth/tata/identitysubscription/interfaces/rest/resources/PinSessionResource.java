package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import java.time.Instant;

/** Issued adult credential returned after successful PIN authentication. */
public record PinSessionResource(String olderAdultId, String accessToken, Instant expiresAt) {
  /** Keeps the issued bearer credential out of diagnostic strings. */
  @Override
  public String toString() {
    return "PinSessionResource[olderAdultId=" + olderAdultId + ", expiresAt=" + expiresAt + "]";
  }
}
