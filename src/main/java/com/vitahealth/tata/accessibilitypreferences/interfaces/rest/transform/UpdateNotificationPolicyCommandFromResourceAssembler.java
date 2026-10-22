package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform;

import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPolicyCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateNotificationPolicyResource;

/** Converts validated timing and ordered recipients into one owner-scoped policy command. */
public final class UpdateNotificationPolicyCommandFromResourceAssembler {
  private UpdateNotificationPolicyCommandFromResourceAssembler() {}

  /** Normalizes recipient values through the domain constructors without persistence access. */
  public static UpdateNotificationPolicyCommand toCommand(
      String userId, UpdateNotificationPolicyResource resource) {
    var policy = resource.policy();
    return new UpdateNotificationPolicyCommand(
        userId,
        new NotificationPolicy(
            policy.reminderMinutes(),
            policy.pendingAlertMinutes(),
            policy.omittedAlertMinutes(),
            policy.escalationMinutes(),
            policy.dailySummaryTime()),
        resource.contacts().stream().map(c -> new EmergencyContact(c.name(), c.phone())).toList());
  }
}
