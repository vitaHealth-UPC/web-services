package com.tata.identitysubscription.interfaces.rest.resources;

import com.tata.identitysubscription.domain.model.valueobjects.PlanCapability;

import java.math.BigDecimal;
import java.util.Set;

public record PlanResource(
        String code,
        String name,
        BigDecimal monthlyPrice,
        String currency,
        Set<PlanCapability> capabilities
) {
}
