package com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens;

import com.vitahealth.tata.identitysubscription.application.result.RegisteredSessionResult;
import java.util.Optional;

/**
 * Looks up revocable registration evidence without exposing credential digests or storage entities.
 */
public interface SessionRegistrationLookup {
  /** Resolves a credential already checked by the token authentication service. */
  Optional<RegisteredSessionResult> findRegistration(String credential);

  /** Rechecks expiration, account state and relationship consent for a stable session reference. */
  Optional<RegisteredSessionResult> findActiveRegistration(String sessionId);
}
