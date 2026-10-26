package com.vitahealth.tata.accessibilitypreferences.domain.model.aggregates;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Accessibility and interaction preferences of one user. The user is referenced by logical id only.
 */
public final class UserPreferences {
  private final String id;
  private final String userId;
  private TextSizeLevel textSize;
  private boolean highContrast;
  private boolean darkTheme;
  private boolean reducedMotion;
  private boolean readingAssistance;
  private boolean voiceConfirmationEnabled;
  private QuietHoursRange quietHours;
  private List<NotificationChannel> notificationChannels;
  private NotificationPolicy notificationPolicy = NotificationPolicy.defaults();
  private List<EmergencyContact> emergencyContacts = List.of();
  private final Instant createdAt;

  private UserPreferences(
      String id,
      String userId,
      TextSizeLevel textSize,
      boolean highContrast,
      boolean reducedMotion,
      boolean readingAssistance,
      boolean voiceConfirmationEnabled,
      QuietHoursRange quietHours,
      List<NotificationChannel> notificationChannels,
      Instant createdAt) {
    if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
    this.id = id;
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId is required");
    }
    this.userId = userId.trim();
    this.textSize = Objects.requireNonNull(textSize, "text size is required");
    this.highContrast = highContrast;
    this.reducedMotion = reducedMotion;
    this.readingAssistance = readingAssistance;
    this.voiceConfirmationEnabled = voiceConfirmationEnabled;
    this.quietHours = quietHours;
    this.notificationChannels = validatedChannels(notificationChannels);
    this.createdAt = Objects.requireNonNull(createdAt);
  }

  /** Restores stable identity and supplied choices without executing a business transition. */
  public static UserPreferences rehydrate(
      String id,
      String userId,
      TextSizeLevel textSize,
      boolean highContrast,
      boolean reducedMotion,
      boolean readingAssistance,
      boolean voiceConfirmationEnabled,
      QuietHoursRange quietHours,
      List<NotificationChannel> notificationChannels,
      Instant createdAt) {
    return new UserPreferences(
        id,
        userId,
        textSize,
        highContrast,
        reducedMotion,
        readingAssistance,
        voiceConfirmationEnabled,
        quietHours,
        notificationChannels,
        createdAt);
  }

  /** Replaces only the text-size choice. */
  public void updateTextSize(TextSizeLevel textSize) {
    this.textSize = Objects.requireNonNull(textSize, "text size is required");
  }

  /** Reconstructs policy settings; the overload above also supports older stored preferences. */
  public static UserPreferences rehydrate(
      String id,
      String userId,
      TextSizeLevel textSize,
      boolean highContrast,
      boolean reducedMotion,
      boolean readingAssistance,
      boolean voiceConfirmationEnabled,
      QuietHoursRange quietHours,
      List<NotificationChannel> channels,
      Instant createdAt,
      NotificationPolicy policy,
      List<EmergencyContact> contacts) {
    var preferences =
        rehydrate(
            id,
            userId,
            textSize,
            highContrast,
            reducedMotion,
            readingAssistance,
            voiceConfirmationEnabled,
            quietHours,
            channels,
            createdAt);
    preferences.updateNotificationPolicy(policy, contacts);
    return preferences;
  }

  /** Validates the complete replacement before changing either part of the aggregate. */
  public void updateNotificationPolicy(NotificationPolicy policy, List<EmergencyContact> contacts) {
    if (policy == null || contacts == null || contacts.size() > 5) {
      throw new IllegalArgumentException("A policy and at most five contacts are required");
    }
    var phones = new HashSet<String>();
    for (var contact : contacts) {
      if (contact == null || !phones.add(contact.phone().replace("+", ""))) {
        throw new IllegalArgumentException("Emergency contacts must have distinct phones");
      }
    }
    this.notificationPolicy = policy;
    this.emergencyContacts = List.copyOf(contacts);
  }

  /** Replaces only the high-contrast choice. */
  public void setHighContrast(boolean enabled) {
    this.highContrast = enabled;
  }

  /** Replaces only the explicit dark-theme choice. */
  public void setDarkTheme(boolean enabled) {
    this.darkTheme = enabled;
  }

  /** Returns the explicit theme choice, defaulting to light for existing users. */
  public boolean darkTheme() {
    return darkTheme;
  }

  /** Replaces only the reduced-motion choice. */
  public void setReducedMotion(boolean enabled) {
    this.reducedMotion = enabled;
  }

  /** Replaces only the reading-assistance choice. */
  public void setReadingAssistance(boolean enabled) {
    this.readingAssistance = enabled;
  }

  /** Replaces only the voice-confirmation choice. */
  public void setVoiceConfirmation(boolean enabled) {
    this.voiceConfirmationEnabled = enabled;
  }

  /** Sets the quiet hours; null removes them. */
  public void setQuietHours(QuietHoursRange quietHours) {
    this.quietHours = quietHours;
  }

  /** Replaces the channel list. Each channel type may appear only once. */
  public void updateChannels(List<NotificationChannel> channels) {
    this.notificationChannels = validatedChannels(channels);
  }

  /** Validates the complete replacement before changing quiet hours or delivery channels. */
  public void updateNotificationPreferences(
      QuietHoursRange quietHours, List<NotificationChannel> channels) {
    var validated = validatedChannels(channels);
    this.quietHours = quietHours;
    this.notificationChannels = validated;
  }

  /** Checks the explicit choice for one channel without creating missing choices. */
  public boolean isChannelEnabled(ChannelType type) {
    return notificationChannels.stream()
        .anyMatch(channel -> channel.type() == type && channel.enabled());
  }

  private static List<NotificationChannel> validatedChannels(List<NotificationChannel> channels) {
    if (channels == null) {
      throw new IllegalArgumentException("channels are required");
    }
    var seen = new HashSet<ChannelType>();
    for (var channel : channels) {
      if (channel == null) {
        throw new IllegalArgumentException("channel must not be null");
      }
      if (!seen.add(channel.type())) {
        throw new IllegalArgumentException("channel " + channel.type() + " appears more than once");
      }
    }
    return List.copyOf(channels);
  }

  /** Returns the stable identity retained across preference changes. */
  public String id() {
    return id;
  }

  /** Returns the logical user who owns these preferences. */
  public String userId() {
    return userId;
  }

  /** Returns the selected text-size level. */
  public TextSizeLevel textSize() {
    return textSize;
  }

  /** Returns the selected high-contrast state. */
  public boolean highContrast() {
    return highContrast;
  }

  /** Returns the selected reduced-motion state. */
  public boolean reducedMotion() {
    return reducedMotion;
  }

  /** Returns the selected reading-assistance state. */
  public boolean readingAssistance() {
    return readingAssistance;
  }

  /** Returns whether this user allows voice confirmation. */
  public boolean voiceConfirmationEnabled() {
    return voiceConfirmationEnabled;
  }

  /** Returns the silence interval, or null when explicitly disabled. */
  public QuietHoursRange quietHours() {
    return quietHours;
  }

  /** Returns immutable channel choices; absent channel types are disabled. */
  public List<NotificationChannel> notificationChannels() {
    return notificationChannels;
  }

  /** Returns the notification timing configuration. */
  public NotificationPolicy notificationPolicy() {
    return notificationPolicy;
  }

  /** Returns immutable emergency contacts in their selected order. */
  public List<EmergencyContact> emergencyContacts() {
    return emergencyContacts;
  }

  /** Returns the original preference creation instant. */
  public Instant createdAt() {
    return createdAt;
  }
}
