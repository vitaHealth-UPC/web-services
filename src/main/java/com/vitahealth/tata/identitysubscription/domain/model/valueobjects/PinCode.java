package com.vitahealth.tata.identitysubscription.domain.model.valueobjects;

/** Transient four-digit adult access secret; persistence stores only its encoded form. */
public record PinCode(String value) {
  /** Requires exactly four ASCII digits, preserving leading zeroes without trimming. */
  public PinCode {
    if (value == null || !value.matches("[0-9]{4}")) {
      throw new IllegalArgumentException("PIN must contain exactly 4 digits");
    }
  }

  /** Keeps the access secret out of diagnostic strings. */
  @Override
  public String toString() {
    return "PinCode[redacted]";
  }
}
