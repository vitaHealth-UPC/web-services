package com.tata.accessibilitypreferences.interfaces.rest.transform;

import com.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPolicyCommand;
import com.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UpdateNotificationPolicyResource;

public final class UpdateNotificationPolicyCommandFromResourceAssembler {
  private UpdateNotificationPolicyCommandFromResourceAssembler() {}

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
