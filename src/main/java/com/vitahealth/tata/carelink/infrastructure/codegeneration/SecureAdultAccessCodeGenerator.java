package com.vitahealth.tata.carelink.infrastructure.codegeneration;

import com.vitahealth.tata.carelink.application.internal.outboundservices.AdultAccessCodeGenerator;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Eight characters from an alphabet without look-alike symbols (no 0/O, 1/I/L), shown as two
 * groups: about 10^12 combinations for a code that is redeemed without a session.
 */
@Component
public class SecureAdultAccessCodeGenerator implements AdultAccessCodeGenerator {
  private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
  private final SecureRandom random = new SecureRandom();

  /** Produces one candidate such as {@code K7PM-Q2XD}. */
  @Override
  public String generate() {
    var value = new StringBuilder(9);
    for (int i = 0; i < 8; i++) {
      if (i == 4) value.append('-');
      value.append(ALPHABET[random.nextInt(ALPHABET.length)]);
    }
    return value.toString();
  }
}
