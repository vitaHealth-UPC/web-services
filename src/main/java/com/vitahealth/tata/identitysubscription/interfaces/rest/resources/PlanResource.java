package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PlanCapability;
import java.math.BigDecimal;
import java.util.Set;

/** Exposes a catalog plan and a snapshot of its available capabilities. */
public record PlanResource(
    String code,
    String name,
    BigDecimal monthlyPrice,
    String currency,
    Set<PlanCapability> capabilities) {
  /** Isolates response capabilities from changes to a caller-owned collection. */
  public PlanResource {
    capabilities = Set.copyOf(capabilities);
  }
}
