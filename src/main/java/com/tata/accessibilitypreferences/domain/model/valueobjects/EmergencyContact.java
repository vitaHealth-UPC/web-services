package com.tata.accessibilitypreferences.domain.model.valueobjects;

import com.tata.shared.domain.validation.DomainText;

/** A user-configured recipient. Saving it does not grant access to health information. */
public record EmergencyContact(String name, String phone) {
  public EmergencyContact {
    name = DomainText.requireText(name, "Contact name");
    if (name.length() > 80) {
      throw new IllegalArgumentException("Contact name must contain between 1 and 80 characters");
    }
    if (phone == null) throw new IllegalArgumentException("Contact phone is required");
    phone = phone.trim().replaceAll("[\\s()\\-]", "");
    if (!phone.matches("\\+?[0-9]{7,15}")) {
      throw new IllegalArgumentException("Contact phone must contain between 7 and 15 digits");
    }
  }
}
