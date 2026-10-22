package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.vitahealth.tata.familymonitoring.interfaces.rest.FamilyMonitoringExceptionHandler;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

/** Verifies complete localized public codes and sanitized concurrent-write responses. */
class FamilyMonitoringErrorCatalogTest {
  private ResourceBundleMessageSource catalog() {
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("i18n/messages");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    return messages;
  }

  @Test
  void everyHandledCodeHasEnglishAndSpanishMessages() {
    var messages = catalog();
    for (String suffix :
        new String[] {
          "monitoring-source-error",
          "monitoring-state-error",
          "concurrent-update",
          "care-relationship-required",
          "not-found",
          "invalid-request",
          "validation-error",
          "malformed-request",
          "invalid-state"
        }) {
      var english = messages.getMessage("family.error." + suffix, null, Locale.ENGLISH);
      var spanish =
          messages.getMessage("family.error." + suffix, null, Locale.forLanguageTag("es-PE"));
      assertFalse(english.isBlank());
      assertFalse(spanish.isBlank());
      assertNotEquals(english, spanish);
    }
  }

  @Test
  void concurrencyResponseRetainsCodeAndStatusWithoutExposingDatabaseDetails() {
    var handler = new FamilyMonitoringExceptionHandler(catalog());
    var source = new OptimisticLockingFailureException("private-database-marker");
    for (Locale locale : new Locale[] {Locale.ENGLISH, Locale.forLanguageTag("es-PE")}) {
      var response = handler.handleConcurrentUpdate(source, locale);
      assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals("CONCURRENT_UPDATE", response.getBody().code());
      assertFalse(response.getBody().message().contains("private-database-marker"));
    }
  }
}
