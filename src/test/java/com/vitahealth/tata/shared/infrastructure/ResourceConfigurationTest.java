package com.vitahealth.tata.shared.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/** Verifies complete UTF-8 catalogs and equivalent environment-driven profile contracts. */
class ResourceConfigurationTest {
  private Properties load(String resource) throws Exception {
    var properties = new Properties();
    try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
      if (stream == null) throw new IllegalArgumentException(resource);
      try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        properties.load(reader);
      }
    }
    return properties;
  }

  @Test
  void everyCatalogEntryResolvesInEnglishAndRegionalSpanish() throws Exception {
    var english = load("i18n/messages.properties");
    var spanish = load("i18n/messages_es.properties");
    assertEquals(english.stringPropertyNames(), spanish.stringPropertyNames());
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("i18n/messages");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    for (String key : english.stringPropertyNames()) {
      assertFalse(english.getProperty(key).isBlank(), key);
      assertFalse(spanish.getProperty(key).isBlank(), key);
      assertEquals(english.getProperty(key), messages.getMessage(key, null, Locale.ENGLISH));
      for (String locale : new String[] {"es", "es-PE", "es-419"}) {
        assertEquals(
            spanish.getProperty(key),
            messages.getMessage(key, null, Locale.forLanguageTag(locale)));
      }
      assertNotEquals(english.getProperty(key), spanish.getProperty(key), key);
    }
    assertTrue(spanish.getProperty("carelink.error.care-link-not-found").contains("vínculo"));
  }

  @Test
  void profilesExposeTheSameKeysWithSafeProductionDefaults() throws Exception {
    var dev = load("application-dev.properties");
    var prod = load("application-prod.properties");
    assertEquals(dev.stringPropertyNames(), prod.stringPropertyNames());
    assertEquals("true", dev.getProperty("spring.jpa.show-sql"));
    assertEquals("false", prod.getProperty("spring.jpa.show-sql"));
    assertEquals("true", dev.getProperty("spring.jpa.properties.hibernate.format_sql"));
    assertEquals("false", prod.getProperty("spring.jpa.properties.hibernate.format_sql"));
    assertEquals(
        dev.getProperty("spring.datasource.url"), prod.getProperty("spring.datasource.url"));
    assertEquals("${TATA_DEMO_SEED:false}", prod.getProperty("tata.demo.seed"));
    var environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    environment
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "database",
                java.util.Map.of(
                    "DATABASE_HOST",
                    "db.internal",
                    "DATABASE_NAME",
                    "tata_prod",
                    "DATABASE_USER",
                    "service")));
    assertEquals(
        "jdbc:postgresql://db.internal:5432/tata_prod",
        environment.resolveRequiredPlaceholders(prod.getProperty("spring.datasource.url")));
    assertEquals(
        "service",
        environment.resolveRequiredPlaceholders(prod.getProperty("spring.datasource.username")));
    assertEquals(
        "",
        environment.resolveRequiredPlaceholders(prod.getProperty("spring.datasource.password")));
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "deployment",
                java.util.Map.of(
                    "SPRING_DATASOURCE_URL",
                    "jdbc:postgresql://managed:5432/live",
                    "SPRING_DATASOURCE_USERNAME",
                    "managed-user")));
    assertEquals(
        "jdbc:postgresql://managed:5432/live",
        environment.resolveRequiredPlaceholders(prod.getProperty("spring.datasource.url")));
    assertEquals(
        "managed-user",
        environment.resolveRequiredPlaceholders(prod.getProperty("spring.datasource.username")));
  }
}
