package com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing;

import com.vitahealth.tata.identitysubscription.application.exceptions.UnsupportedCredentialException;

/** Encodes transient credentials without coupling application callers to a hashing technology. */
public interface PasswordHasher {
  /**
   * Encodes a credential without truncation or normalization.
   *
   * @throws UnsupportedCredentialException when the configured algorithm cannot safely represent
   *     the supplied credential
   */
  String hash(String rawValue);

  /** Checks the exact credential against its encoded form; unsupported input cannot match. */
  boolean matches(String rawValue, String encodedValue);
}
