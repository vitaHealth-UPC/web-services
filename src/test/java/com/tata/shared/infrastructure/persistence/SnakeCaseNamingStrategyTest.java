package com.tata.shared.infrastructure.persistence;

import com.tata.shared.infrastructure.persistence.jpa.configuration.strategy.SnakeCaseWithPluralizedTablePhysicalNamingStrategy;
import java.util.Locale;
import org.hibernate.boot.model.naming.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SnakeCaseNamingStrategyTest {
    @Test
    void columnNamesRemainStableAcrossLocalesAndKeepQuoting() {
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            var strategy = new SnakeCaseWithPluralizedTablePhysicalNamingStrategy();
            var column = strategy.toPhysicalColumnName(Identifier.toIdentifier("IntakeId", true), null);
            assertEquals("intake_id", column.getText());
            assertTrue(column.isQuoted());
        } finally {
            Locale.setDefault(previous);
        }
    }
}
