package com.vitahealth.tata.identitysubscription.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalized account email identity within the persistence contract's supported length. */
public record EmailAddress(String value) {
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

  /** Rejects missing or unsupported addresses with the domain validation exception contract. */
  public EmailAddress {
    value = requireText(value, "email").toLowerCase(Locale.ROOT);
    if (value.length() > 200 || !EMAIL_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("email must be valid");
    }
  }

  /** Returns the normalized address when explicitly formatting this value object. */
  @Override
  public String toString() {
    return value;
  }
}
