package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.adherenceanalytics.application.exceptions.InvalidAdherenceSourceException;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.AdherenceExceptionHandler;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Checks stable source-error status and localized safe messages without leaking provider details.
 */
class AdherenceErrorLocalizationTest {
  @Test
  void sourceFailureUsesSafeLocalizedServerError() {
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("i18n/messages");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    var handler = new AdherenceExceptionHandler(messages);
    var failure =
        new InvalidAdherenceSourceException(new IllegalArgumentException("private source data"));
    var spanish = handler.invalidSource(failure, Locale.forLanguageTag("es"));
    var english = handler.invalidSource(failure, Locale.ENGLISH);
    assertEquals(500, spanish.getStatus());
    assertEquals("No se pudo consultar la información de adherencia", spanish.getDetail());
    assertEquals("Unable to retrieve adherence evidence", english.getDetail());
    assertFalse(spanish.getDetail().contains("private"));
  }
}
