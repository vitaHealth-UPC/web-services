package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.application.result.PlanResult;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.PlanResource;

/** Projects plan catalog results into public plan resources. */
public final class PlanResourceFromResultAssembler {
  private PlanResourceFromResultAssembler() {}

  /** Preserves price, currency and capabilities without applying billing rules. */
  public static PlanResource toResource(PlanResult result) {
    return new PlanResource(
        result.code(),
        result.name(),
        result.monthlyPrice(),
        result.currency(),
        result.capabilities());
  }
}
