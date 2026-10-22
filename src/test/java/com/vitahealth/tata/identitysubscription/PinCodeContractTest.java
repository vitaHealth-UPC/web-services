package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PinCode;
import org.junit.jupiter.api.Test;

/** Verifies the shared domain PIN contract used by enrollment and authentication. */
class PinCodeContractTest {
  @Test
  void preservesLeadingZeroesAndRedactsDiagnostics() {
    var pin = new PinCode("0123");
    assertEquals("0123", pin.value());
    assertFalse(pin.toString().contains("0123"));
  }

  @Test
  void rejectsWhitespaceUnicodeDigitsAndInvalidLength() {
    for (String input : new String[] {null, "", "123", "12345", " 1234", "１２３４", "12x4"}) {
      assertThrows(IllegalArgumentException.class, () -> new PinCode(input));
    }
  }
}
