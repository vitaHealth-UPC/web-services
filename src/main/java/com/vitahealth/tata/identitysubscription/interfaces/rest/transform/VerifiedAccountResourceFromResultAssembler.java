package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.application.result.VerifiedAccountResult;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.AccountResource;

/** Projects the verified account and newly issued session into the verification response. */
public final class VerifiedAccountResourceFromResultAssembler {
  private VerifiedAccountResourceFromResultAssembler() {}

  /** Preserves the public account response with optional issued credential fields. */
  public static AccountResource toResource(VerifiedAccountResult result) {
    var account = result.account();
    var session = result.session();
    return new AccountResource(
        account.id(),
        account.name(),
        account.email(),
        account.status().name(),
        session.accessToken(),
        session.expiresAt());
  }
}
