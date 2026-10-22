package com.vitahealth.tata.accessibilitypreferences.application.internal.assemblers;

import com.vitahealth.tata.accessibilitypreferences.application.result.NotificationPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;

/** Projects the delivery capabilities consumed by other contexts without leaking mutable state. */
public final class NotificationPreferencesResultFromResultAssembler {
  private NotificationPreferencesResultFromResultAssembler() {}

  /** Preserves channel choices and a deliberately absent quiet interval. */
  public static NotificationPreferencesResult toResult(UserPreferencesResult preferences) {
    var quietHours = preferences.quietHours();
    return new NotificationPreferencesResult(
        preferences.isChannelEnabled(ChannelType.PUSH),
        preferences.isChannelEnabled(ChannelType.SMS),
        preferences.isChannelEnabled(ChannelType.EMAIL),
        quietHours == null ? null : quietHours.start(),
        quietHours == null ? null : quietHours.end());
  }
}
