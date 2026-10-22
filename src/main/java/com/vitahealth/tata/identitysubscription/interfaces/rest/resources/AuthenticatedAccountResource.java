package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import java.time.Instant;

/** Sign-in response containing the caller's bearer credential and expiration. */
public record AuthenticatedAccountResource(
    String accountId, String accessToken, Instant expiresAt) {
  /** Omits the bearer credential from diagnostic string representations. */
  @Override
  public String toString() {
    return "AuthenticatedAccountResource[accountId=" + accountId + ", expiresAt=" + expiresAt + "]";
  }
}
