package com.vitahealth.tata.carelink.application.result;

import java.time.Instant;
import java.util.Objects;

/** Relationship outcome with optional issued access; rejected consent contains no credential. */
public record AuthenticatedCareLinkResult(
    CareLinkResult link, String accessToken, Instant expiresAt) {
  /** Requires complete credential evidence when an access token is present. */
  public AuthenticatedCareLinkResult {
    Objects.requireNonNull(link, "link");
    if ((accessToken == null) != (expiresAt == null)
        || accessToken != null && accessToken.isBlank()) {
      throw new IllegalArgumentException("access token and expiration must be present together");
    }
  }

  /** Omits the bearer credential from diagnostic output. */
  @Override
  public String toString() {
    return "AuthenticatedCareLinkResult[linkId="
        + link.id()
        + ", accessToken=REDACTED, expiresAt="
        + expiresAt
        + "]";
  }
}
