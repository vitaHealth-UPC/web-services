package com.vitahealth.tata.familymonitoring.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.util.Objects;

/** Channel the caregiver can use to reach the older adult when an alert is open. */
public record ContactChannel(ContactChannelType type, String value) {
  /** Requires the registered contact evidence without changing its source formatting. */
  public ContactChannel {
    Objects.requireNonNull(type, "type");
    requireText(value, "contact destination");
  }
}
