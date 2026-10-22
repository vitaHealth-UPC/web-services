package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import java.util.List;

/** Replaces delivery timing and emergency recipients together, without changing channels. */
public record UpdateNotificationPolicyCommand(
    String userId, NotificationPolicy policy, List<EmergencyContact> contacts) {
  /** Captures the intended owner and recipients before any mutation begins. */
  public UpdateNotificationPolicyCommand {
    userId = requireText(userId, "userId");
    if (policy == null) throw new IllegalArgumentException("policy is required");
    if (contacts == null || contacts.stream().anyMatch(contact -> contact == null))
      throw new IllegalArgumentException("contacts must contain nonnull recipients");
    contacts = List.copyOf(contacts);
  }
}
