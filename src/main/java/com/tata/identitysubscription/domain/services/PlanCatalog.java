package com.tata.identitysubscription.domain.services;

import com.tata.identitysubscription.domain.model.entities.Plan;
import com.tata.identitysubscription.domain.model.valueobjects.PlanCapability;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class PlanCatalog {
    public static final String ESSENTIAL = "ESSENTIAL";
    public static final String FAMILY = "FAMILY";

    private static final List<Plan> PLANS = List.of(
            new Plan(
                    ESSENTIAL,
                    "Esencial",
                    new BigDecimal("9.90"),
                    "PEN",
                    Set.of(
                            PlanCapability.REMINDERS,
                            PlanCapability.AGENDA,
                            PlanCapability.INTAKE_CONFIRMATION
                    )
            ),
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
                            PlanCapability.ADHERENCE_INSIGHTS
                    )
            )
    );

    private PlanCatalog() {
    }

    public static List<Plan> availablePlans() {
        return PLANS;
    }

    public static Optional<Plan> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return PLANS.stream().filter(plan -> plan.code().equals(normalized)).findFirst();
    }
}
