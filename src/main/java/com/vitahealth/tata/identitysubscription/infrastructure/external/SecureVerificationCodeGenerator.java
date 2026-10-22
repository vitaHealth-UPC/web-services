package com.vitahealth.tata.identitysubscription.infrastructure.external;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationCodeGenerator;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Generates six-digit verification codes using a cryptographically secure random source. */
@Component
public class SecureVerificationCodeGenerator implements VerificationCodeGenerator {
  private final SecureRandom random = new SecureRandom();

  /** Retains leading zeroes and uses locale-independent decimal digits. */
  @Override
  public String generate() {
    return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
  }
}
