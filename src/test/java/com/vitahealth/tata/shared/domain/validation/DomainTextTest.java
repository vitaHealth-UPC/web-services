package com.vitahealth.tata.shared.domain.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Checks shared text normalization without changing meaningful internal content. */
class DomainTextTest {
  @Test
  void stripsUnicodeWhitespaceRecognizedByBlankValidation() {
    assertEquals("Rosa Vargas", DomainText.requireText("\u2003Rosa Vargas\u2003", "name"));
    assertThrows(IllegalArgumentException.class, () -> DomainText.requireText("\u2003", "name"));
  }

  @Test
  void preservesInternalSpacingAndNonbreakingSpaces() {
    assertEquals("Rosa  Vargas", DomainText.requireText(" Rosa  Vargas ", "name"));
    assertEquals("\u00a0Rosa\u00a0", DomainText.requireText("\u00a0Rosa\u00a0", "name"));
  }

  @Test
  void rejectsMissingValues() {
    assertThrows(IllegalArgumentException.class, () -> DomainText.requireText(null, "name"));
    assertThrows(IllegalArgumentException.class, () -> DomainText.requireText("  ", "name"));
  }
}
