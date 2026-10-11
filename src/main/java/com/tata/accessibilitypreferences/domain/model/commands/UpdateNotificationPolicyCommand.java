package com.tata.accessibilitypreferences.domain.model.commands;

import com.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import java.util.List;
import java.util.Objects;

/** Replaces delivery timing and emergency recipients together, without changing channels. */
public record UpdateNotificationPolicyCommand(
    String userId, NotificationPolicy policy, List<EmergencyContact> contacts) {
  public UpdateNotificationPolicyCommand {
    if (userId == null || userId.isBlank()) throw new IllegalArgumentException("User is required");
    Objects.requireNonNull(policy, "Policy is required");
    if (contacts == null) throw new IllegalArgumentException("Contacts are required");
    contacts = List.copyOf(contacts);
  }
}
