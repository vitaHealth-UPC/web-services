package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import org.junit.jupiter.api.Test;

/** Verifies normalization and predictable domain validation of account email identifiers. */
class EmailAddressContractTest {
  @Test
  void missingOrOversizedEmailHasTheExpectedValidationException() {
    assertThrows(IllegalArgumentException.class, () -> new EmailAddress(null));
    assertThrows(IllegalArgumentException.class, () -> new EmailAddress("  "));
    assertThrows(
        IllegalArgumentException.class, () -> new EmailAddress("a".repeat(200) + "@example.com"));
  }

  @Test
  void canonicalizesCaseAndSharedDomainWhitespace() {
    assertEquals("rosa@example.com", new EmailAddress("\u2003ROSA@EXAMPLE.COM\u2003").value());
  }
}
