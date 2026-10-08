package com.tata.identitysubscription.domain.model;

import com.tata.identitysubscription.domain.model.entities.Plan;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanCodeNormalizationTest {
    @Test
    void machineCodesRemainStableUnderTurkishServerLocale() {
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            var plan = new Plan("essential", "Essential", BigDecimal.ZERO, "inr", Set.of());
            assertEquals("ESSENTIAL", plan.code());
            assertEquals("INR", plan.currency());
        } finally {
            Locale.setDefault(previous);
        }
    }
}
