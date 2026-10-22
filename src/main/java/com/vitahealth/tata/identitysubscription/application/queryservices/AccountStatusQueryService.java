package com.vitahealth.tata.identitysubscription.application.queryservices;

/** Publishes the live account eligibility check used by other context boundaries. */
public interface AccountStatusQueryService {
  boolean isEnabled(String accountId);
}
