package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import java.security.Principal;
import java.util.Objects;

/** Adapts verified application identity to the security framework's request principal. */
public record SessionPrincipal(AuthenticatedSession session) implements Principal {
  /** Requires a verified identity rather than an anonymous principal. */
  public SessionPrincipal {
    Objects.requireNonNull(session, "session");
  }

  /** Returns the authenticated subject identifier used by Spring request authorization. */
  @Override
  public String getName() {
    return session.subjectId();
  }
}
