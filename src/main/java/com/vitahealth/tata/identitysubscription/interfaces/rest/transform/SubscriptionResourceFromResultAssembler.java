package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.application.result.SubscriptionResult;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.SubscriptionResource;

/** Projects subscription state into its HTTP response contract. */
public final class SubscriptionResourceFromResultAssembler {
  private SubscriptionResourceFromResultAssembler() {}

  /** Delegates nested plan projection while preserving the application renewal date. */
  public static SubscriptionResource toResource(SubscriptionResult result) {
    return new SubscriptionResource(
        result.accountId(),
        PlanResourceFromResultAssembler.toResource(result.plan()),
        result.status(),
        result.renewsAt());
  }
}
