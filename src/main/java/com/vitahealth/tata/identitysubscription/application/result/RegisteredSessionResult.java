package com.vitahealth.tata.identitysubscription.application.result;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import java.util.Objects;

/**
 * Stable revocable session reference for owned device registrations, without bearer credentials.
 */
public record RegisteredSessionResult(String sessionId, AuthenticatedSession identity) {
  /** Requires the stored identity and the authorization checked for this lookup. */
  public RegisteredSessionResult {
    if (sessionId == null || sessionId.isBlank())
      throw new IllegalArgumentException("session id is required");
    Objects.requireNonNull(identity, "identity");
  }
}
