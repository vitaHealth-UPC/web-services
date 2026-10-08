package com.tata.identitysubscription.application.models;

import com.tata.identitysubscription.domain.model.valueobjects.PlanCapability;

import java.math.BigDecimal;
import java.util.Set;

public record PlanResult(
        String code,
        String name,
        BigDecimal monthlyPrice,
        String currency,
        Set<PlanCapability> capabilities
) {
}
