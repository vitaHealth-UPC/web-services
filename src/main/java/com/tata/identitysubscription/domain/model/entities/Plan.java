package com.tata.identitysubscription.domain.model.entities;

import com.tata.identitysubscription.domain.model.valueobjects.PlanCapability;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

public record Plan(
        String code,
        String name,
        BigDecimal monthlyPrice,
        String currency,
        Set<PlanCapability> capabilities
) {
    public Plan {
        code = requireText(code, "code").toUpperCase();
        name = requireText(name, "name");
        monthlyPrice = Objects.requireNonNull(monthlyPrice, "monthlyPrice is required");
        if (monthlyPrice.signum() < 0) {
            throw new IllegalArgumentException("monthlyPrice cannot be negative");
        }
        currency = requireText(currency, "currency").toUpperCase();
        capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities are required"));
    }

    public boolean supports(PlanCapability capability) {
        return capabilities.contains(Objects.requireNonNull(capability));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
