package com.vitahealth.tata.identitysubscription.infrastructure.external;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.carelink.infrastructure.codegeneration.SecureLinkingCodeGenerator;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Codes retain the ASCII digit format accepted by API and mobile validation. */
class CodeGeneratorLocaleTest {
  @Test
  void codesUseAsciiDigitsWithArabicServerLocale() {
    var previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("ar-EG"));
      var verification = new SecureVerificationCodeGenerator();
      var linking = new SecureLinkingCodeGenerator();
      for (int index = 0; index < 100; index++) {
        assertTrue(verification.generate().matches("[0-9]{6}"));
        assertTrue(linking.generate().matches("TATA-[0-9]{4}"));
      }
    } finally {
      Locale.setDefault(previous);
    }
  }
}
