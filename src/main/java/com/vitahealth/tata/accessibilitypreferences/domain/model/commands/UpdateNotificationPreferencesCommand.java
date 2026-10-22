package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import java.util.List;

/** Replaces one user's quiet interval and channel choices as a single preference decision. */
public record UpdateNotificationPreferencesCommand(
    String userId, QuietHoursRange quietHours, List<NotificationChannel> channels) {
  /** A null interval removes silence; channel uniqueness is validated by the aggregate. */
  public UpdateNotificationPreferencesCommand {
    userId = requireText(userId, "userId");
    if (channels == null || channels.stream().anyMatch(channel -> channel == null))
      throw new IllegalArgumentException("channels must contain nonnull choices");
    channels = List.copyOf(channels);
  }
}
