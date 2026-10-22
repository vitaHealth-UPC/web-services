package com.vitahealth.tata.shared.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.shared.infrastructure.persistence.jpa.configuration.strategy.SnakeCaseWithPluralizedTablePhysicalNamingStrategy;
import java.util.Locale;
import org.hibernate.boot.model.naming.Identifier;
import org.junit.jupiter.api.Test;

class SnakeCaseNamingStrategyTest {
  @Test
  void explicitCatalogIsPreservedIncludingQuoting() {
    var strategy = new SnakeCaseWithPluralizedTablePhysicalNamingStrategy();
    var catalog = Identifier.toIdentifier("ClinicalCatalog", true);
    assertSame(catalog, strategy.toPhysicalCatalogName(catalog, null));
    assertNull(strategy.toPhysicalCatalogName(null, null));
  }

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
