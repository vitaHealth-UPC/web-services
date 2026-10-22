package com.vitahealth.tata.identitysubscription.application.queryservices;

import com.vitahealth.tata.identitysubscription.application.result.SubscriptionResult;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/** Read contract for an account's current plan, renewal and capabilities. */
public interface GetCurrentSubscriptionQueryService {
  /** Returns missing-account or missing-plan failures without altering subscription state. */
  Result<SubscriptionResult, ApplicationError> handle(String accountId);
}
