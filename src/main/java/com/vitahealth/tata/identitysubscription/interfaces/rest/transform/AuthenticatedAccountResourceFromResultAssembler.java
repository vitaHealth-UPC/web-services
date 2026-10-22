package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.AuthenticatedAccountResource;

/** Projects the SessionResult application outcome into its public HTTP contract. */
public final class AuthenticatedAccountResourceFromResultAssembler {
  private AuthenticatedAccountResourceFromResultAssembler() {}

  /** Exposes only the fields defined by the response contract. */
  public static AuthenticatedAccountResource toResource(SessionResult result) {
    return new AuthenticatedAccountResource(
        result.accountId(), result.accessToken(), result.expiresAt());
  }
}
