package com.vitahealth.tata.identitysubscription.application.result;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PlanCapability;
import java.math.BigDecimal;
import java.util.Set;

/** Public plan projection with an immutable snapshot of its enabled capabilities. */
public record PlanResult(
    String code,
    String name,
    BigDecimal monthlyPrice,
    String currency,
    Set<PlanCapability> capabilities) {
  /** Copies capabilities so external mutation cannot change an already calculated result. */
  public PlanResult {
    capabilities = Set.copyOf(capabilities);
  }
}
