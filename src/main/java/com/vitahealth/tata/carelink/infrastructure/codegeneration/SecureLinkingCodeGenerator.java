package com.vitahealth.tata.carelink.infrastructure.codegeneration;

import com.vitahealth.tata.carelink.application.internal.outboundservices.LinkingCodeGenerator;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Generates short, locale-independent linking values using cryptographic randomness. */
@Component
public class SecureLinkingCodeGenerator implements LinkingCodeGenerator {
  private final SecureRandom random = new SecureRandom();

  /** Produces one candidate; the issuance transaction owns collision handling and uniqueness. */
  @Override
  public String generate() {
    return String.format(Locale.ROOT, "TATA-%04d", random.nextInt(10_000));
  }
}
