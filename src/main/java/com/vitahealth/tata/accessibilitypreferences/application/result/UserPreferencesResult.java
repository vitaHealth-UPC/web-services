package com.vitahealth.tata.accessibilitypreferences.application.result;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import java.time.Instant;
import java.util.List;

/** Immutable application snapshot independent of aggregate mutation and persistence entities. */
public record UserPreferencesResult(
    String id,
    String userId,
    TextSizeLevel textSize,
    boolean highContrast,
    boolean reducedMotion,
    boolean readingAssistance,
    boolean voiceConfirmationEnabled,
    QuietHoursRange quietHours,
    List<NotificationChannel> notificationChannels,
    Instant createdAt,
    NotificationPolicy notificationPolicy,
    List<EmergencyContact> emergencyContacts) {
  /** Owns collection snapshots so later aggregate or caller changes cannot alter this outcome. */
  public UserPreferencesResult {
    notificationChannels = List.copyOf(notificationChannels);
    emergencyContacts = List.copyOf(emergencyContacts);
  }

  /** Reads one channel from this snapshot; an absent channel is disabled. */
  public boolean isChannelEnabled(ChannelType type) {
    return notificationChannels.stream()
        .anyMatch(channel -> channel.type() == type && channel.enabled());
  }
}
