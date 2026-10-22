package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.application.result.AccountResult;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.AccountResource;

/** Projects the AccountResult application outcome into its public HTTP contract. */
public final class AccountResourceFromResultAssembler {
  private AccountResourceFromResultAssembler() {}

  /** Exposes only the fields defined by the response contract. */
  public static AccountResource toResource(AccountResult result) {
    return new AccountResource(result.id(), result.name(), result.email(), result.status().name());
  }
}
