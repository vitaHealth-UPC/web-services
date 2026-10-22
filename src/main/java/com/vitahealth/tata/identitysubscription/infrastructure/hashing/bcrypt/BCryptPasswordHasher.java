package com.vitahealth.tata.identitysubscription.infrastructure.hashing.bcrypt;

import com.vitahealth.tata.identitysubscription.application.exceptions.UnsupportedCredentialException;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Hashes account passwords and PINs with BCrypt cost 12; raw credentials are never persisted. */
@Component
public class BCryptPasswordHasher implements PasswordHasher {
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

  @Override
  public String hash(String rawValue) {
    if (rawValue == null || rawValue.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new UnsupportedCredentialException();
    }
    return encoder.encode(rawValue);
  }

  @Override
  public boolean matches(String rawValue, String encodedValue) {
    return rawValue != null
        && encodedValue != null
        && rawValue.getBytes(StandardCharsets.UTF_8).length <= 72
        && encoder.matches(rawValue, encodedValue);
  }
}
