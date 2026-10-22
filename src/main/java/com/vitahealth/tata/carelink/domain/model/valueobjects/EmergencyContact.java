package com.vitahealth.tata.carelink.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Immutable contact information with limits matching the profile's persisted columns. */
public record EmergencyContact(String name, String relationship, String phone) {
  /** Requires a complete contact rather than accepting a partially populated record. */
  public EmergencyContact {
    name = requireText(name, "emergency contact name");
    relationship = requireText(relationship, "emergency contact relationship");
    phone = requireText(phone, "emergency contact phone");
    if (name.length() > 160 || relationship.length() > 80 || phone.length() > 40) {
      throw new IllegalArgumentException("emergency contact exceeds supported field lengths");
    }
  }

  /** Keeps contact identity and telephone data out of diagnostic output. */
  @Override
  public String toString() {
    return "EmergencyContact[personalData=REDACTED]";
  }
}
