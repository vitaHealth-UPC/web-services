package com.vitahealth.tata.identitysubscription.application.queryservices;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;

/** Evaluates role restrictions and resource ownership for a verified application identity. */
public interface ResourceAuthorizationQueryService {
  /** Rejects routes outside the scope of a restricted relationship setup credential. */
  void authorizeRequest(AuthenticatedSession session, String path, String method);

  /** Resolves supported resource selectors and rejects ownership that cannot be established. */
  void authorizeSelector(
      AuthenticatedSession session, String selector, String value, String path, String method);
}
