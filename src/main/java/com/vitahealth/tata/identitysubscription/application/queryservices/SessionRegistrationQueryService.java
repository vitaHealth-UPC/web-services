package com.vitahealth.tata.identitysubscription.application.queryservices;

import com.vitahealth.tata.identitysubscription.application.result.RegisteredSessionResult;
import java.util.Optional;

/** Resolves device-registration ownership and revalidates persisted session references. */
public interface SessionRegistrationQueryService {
  /** Authenticates a bearer credential before revealing its stable internal session reference. */
  Optional<RegisteredSessionResult> authenticateRegistration(String credential);

  /** Revalidates a previously authenticated registration without storing its bearer credential. */
  Optional<RegisteredSessionResult> findActiveRegistration(String sessionId);
}
