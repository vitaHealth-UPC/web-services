package com.vitahealth.tata.identitysubscription.domain.model.entities;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PlanCapability;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Immutable catalog entity defining a plan's price, currency and enabled product capabilities. */
public record Plan(
    String code,
    String name,
    BigDecimal monthlyPrice,
    String currency,
    Set<PlanCapability> capabilities) {
  /** Normalizes catalog identifiers and protects the capabilities from external mutation. */
  public Plan {
    code = requireText(code, "code").toUpperCase(Locale.ROOT);
    name = requireText(name, "name");
    monthlyPrice = Objects.requireNonNull(monthlyPrice, "monthlyPrice is required");
    if (monthlyPrice.signum() < 0) {
      throw new IllegalArgumentException("monthlyPrice cannot be negative");
    }
    currency = requireText(currency, "currency").toUpperCase(Locale.ROOT);
    capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities are required"));
  }

  /** Reports whether this plan enables the requested product capability. */
  public boolean supports(PlanCapability capability) {
    return capabilities.contains(Objects.requireNonNull(capability));
  }
}
