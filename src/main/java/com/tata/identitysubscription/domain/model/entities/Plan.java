package com.tata.identitysubscription.domain.model.entities;

import static com.tata.shared.domain.validation.DomainText.requireText;

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
        code = requireText(code, "code").toUpperCase(java.util.Locale.ROOT);
        name = requireText(name, "name");
        monthlyPrice = Objects.requireNonNull(monthlyPrice, "monthlyPrice is required");
        if (monthlyPrice.signum() < 0) {
            throw new IllegalArgumentException("monthlyPrice cannot be negative");
        }
        currency = requireText(currency, "currency").toUpperCase(java.util.Locale.ROOT);
        capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities are required"));
    }

    public boolean supports(PlanCapability capability) {
        return capabilities.contains(Objects.requireNonNull(capability));
    }

}
