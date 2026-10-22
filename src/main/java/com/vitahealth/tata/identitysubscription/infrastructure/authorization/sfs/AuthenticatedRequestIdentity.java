package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.exceptions.ResourceAccessDenied;
import org.springframework.security.core.context.SecurityContextHolder;

/** Resolves the verified session required by private MVC resource authorization. */
final class AuthenticatedRequestIdentity {
  private AuthenticatedRequestIdentity() {}

  /** Rejects absent, anonymous, unauthenticated and foreign principal types. */
  static AuthenticatedSession require() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof SessionPrincipal principal)) {
      throw new ResourceAccessDenied();
    }
    return principal.session();
  }
}
