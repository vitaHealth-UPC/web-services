package com.vitahealth.tata.carelink.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Immutable profile data with persistence-compatible text; temporal rules belong to its aggregate.
 */
public record OlderAdultBasicData(String fullName, LocalDate birthDate) {
  /** Normalizes the name and requires a date without consulting the system clock on hydration. */
  public OlderAdultBasicData {
    fullName = requireText(fullName, "fullName");
    if (fullName.length() > 160)
      throw new IllegalArgumentException("fullName exceeds 160 characters");
    Objects.requireNonNull(birthDate, "birthDate is required");
  }

  /** Omits names and dates of birth from diagnostic output. */
  @Override
  public String toString() {
    return "OlderAdultBasicData[personalData=REDACTED]";
  }
}
