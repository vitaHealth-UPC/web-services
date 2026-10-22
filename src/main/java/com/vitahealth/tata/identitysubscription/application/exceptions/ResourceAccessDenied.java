package com.vitahealth.tata.identitysubscription.application.exceptions;

/** Rejects access when a verified session lacks ownership or a required active care link. */
public final class ResourceAccessDenied extends RuntimeException {
  /** Creates the public access-denied failure without disclosing private resource details. */
  public ResourceAccessDenied() {
    super("The authenticated session cannot access this resource");
  }
}
