package com.vitahealth.tata.identitysubscription.application.result;

import java.time.Instant;

/** Issued credential returned to its owner; the token must not appear in diagnostics. */
public record SessionResult(String accountId, String accessToken, Instant expiresAt) {
  /** Redacts the credential while preserving normal record accessors and JSON serialization. */
  @Override
  public String toString() {
    return "SessionResult[accountId="
        + accountId
        + ", accessToken=<redacted>, expiresAt="
        + expiresAt
        + "]";
  }
}
