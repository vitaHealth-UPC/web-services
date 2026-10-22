package com.vitahealth.tata.identitysubscription.domain.services;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.identitysubscription.domain.model.entities.Plan;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PlanCapability;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Defines the immutable product plan catalog used by subscription commands and queries. */
public final class PlanCatalog {
  /** Identifier of the individual medication support plan. */
  public static final String ESSENTIAL = "ESSENTIAL";

  /** Identifier of the plan including family monitoring and adherence insights. */
  public static final String FAMILY = "FAMILY";

  private static final List<Plan> PLANS =
      List.of(
          new Plan(
              ESSENTIAL,
              "Esencial",
              new BigDecimal("9.90"),
              "PEN",
              Set.of(
                  PlanCapability.REMINDERS,
                  PlanCapability.AGENDA,
                  PlanCapability.INTAKE_CONFIRMATION)),
          new Plan(
              FAMILY,
              "Familiar",
              new BigDecimal("19.90"),
              "PEN",
              Set.of(
                  PlanCapability.REMINDERS,
                  PlanCapability.AGENDA,
                  PlanCapability.INTAKE_CONFIRMATION,
                  PlanCapability.FAMILY_ALERTS,
                  PlanCapability.FAMILY_MONITORING,
                  PlanCapability.ADHERENCE_INSIGHTS)));

  private PlanCatalog() {}

  /** Returns the immutable catalog in its presentation order. */
  public static List<Plan> availablePlans() {
    return PLANS;
  }

  /** Resolves a code using the same text normalization as the plan entity. */
  public static Optional<Plan> findByCode(String code) {
    final String normalized;
    try {
      normalized = requireText(code, "code").toUpperCase(Locale.ROOT);
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
    return PLANS.stream().filter(plan -> plan.code().equals(normalized)).findFirst();
  }
}
