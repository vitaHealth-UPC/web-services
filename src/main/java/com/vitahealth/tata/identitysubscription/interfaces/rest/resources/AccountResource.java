package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import java.time.Instant;

/** Public account projection with optional credential fields after successful verification. */
public record AccountResource(
    String id, String name, String email, String status, String accessToken, Instant expiresAt) {
  /** Creates the account-only response used before a credential is issued. */
  public AccountResource(String id, String name, String email, String status) {
    this(id, name, email, status, null, null);
  }

  /** Keeps account personal data and issued bearer credentials out of diagnostics. */
  @Override
  public String toString() {
    return "AccountResource[id=" + id + ", status=" + status + ", expiresAt=" + expiresAt + "]";
  }
}
