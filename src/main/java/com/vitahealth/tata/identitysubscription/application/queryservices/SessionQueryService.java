package com.vitahealth.tata.identitysubscription.application.queryservices;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import java.util.Optional;

/** Defines the identity lookup required by the authentication pipeline. */
public interface SessionQueryService {
  /** Returns empty for missing, expired, revoked or unauthorized credentials. */
  Optional<AuthenticatedSession> authenticate(String token);
}
