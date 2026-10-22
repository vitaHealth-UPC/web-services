package com.vitahealth.tata.accessibilitypreferences.domain.model.commands;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import java.util.List;

/** Replaces one user's channels with an immutable snapshot of the submitted choices. */
public record UpdateNotificationChannelsCommand(String userId, List<NotificationChannel> channels) {
  /** Requires explicit channel choices; duplicate-type validation belongs to the aggregate. */
  public UpdateNotificationChannelsCommand {
    userId = requireText(userId, "userId");
    if (channels == null || channels.stream().anyMatch(channel -> channel == null))
      throw new IllegalArgumentException("channels must contain nonnull choices");
    channels = List.copyOf(channels);
  }
}
