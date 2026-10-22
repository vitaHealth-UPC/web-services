package com.vitahealth.tata.identitysubscription.domain.services;

/** Registration policy for account passwords, independent of hashing and transport technology. */
public final class PasswordPolicy {
  private PasswordPolicy() {}

  /** Requires the existing minimum length and meaningful input without modifying its contents. */
  public static void requireValidRegistrationPassword(String password) {
    if (password == null || password.isBlank() || password.length() < 8) {
      throw new IllegalArgumentException("password must contain at least 8 characters");
    }
  }
}
