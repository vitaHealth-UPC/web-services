package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Enforces the product's explicit Spanish/English voice locale contract. */
class VoiceConfirmationLanguageTest {
  @Test
  void preservesSupportedLocalesInCanonicalCase() {
    assertEquals("es-419", new VoiceConfirmationLanguage(" ES-419 ").code());
    assertEquals("es-PE", new VoiceConfirmationLanguage("es-pe").code());
    assertEquals("es-ES", new VoiceConfirmationLanguage("es-es").code());
    assertEquals("en-US", new VoiceConfirmationLanguage("en-us").code());
  }

  @Test
  void rejectsUnknownOrMissingLanguagesInsteadOfGuessingSpanish() {
    for (var input : List.of("", "english", "fr-FR", "es", "en", "es_419"))
      assertThrows(IllegalArgumentException.class, () -> new VoiceConfirmationLanguage(input));
    assertThrows(IllegalArgumentException.class, () -> new VoiceConfirmationLanguage(null));
  }
}
