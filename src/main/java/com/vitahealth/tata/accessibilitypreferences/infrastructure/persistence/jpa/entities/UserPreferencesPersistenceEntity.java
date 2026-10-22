package com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables.EmergencyContactEmbeddable;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables.NotificationChannelEmbeddable;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables.NotificationPolicyEmbeddable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Stores preference state while keeping identity and original creation evidence stable. */
@Entity
@Table(name = "user_preferences")
public class UserPreferencesPersistenceEntity {
  @Id private String id;

  @Column(name = "user_id", nullable = false, unique = true, length = 64)
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "text_size", nullable = false, length = 20)
  private TextSizeLevel textSize;

  @Column(name = "high_contrast", nullable = false)
  private boolean highContrast;

  @Column(name = "dark_theme")
  private Boolean darkTheme;

  @Column(name = "reduced_motion", nullable = false)
  private boolean reducedMotion;

  @Column(name = "reading_assistance", nullable = false)
  private boolean readingAssistance;

  @Column(name = "voice_confirmation_enabled", nullable = false)
  private boolean voiceConfirmationEnabled;

  @Column(name = "quiet_hours_start")
  private LocalTime quietHoursStart;

  @Column(name = "quiet_hours_end")
  private LocalTime quietHoursEnd;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "user_notification_channels",
      joinColumns = @JoinColumn(name = "user_preferences_id"))
  private List<NotificationChannelEmbeddable> channels = new ArrayList<>();

  @Embedded private NotificationPolicyEmbeddable notificationPolicy;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "user_emergency_contacts",
      joinColumns = @JoinColumn(name = "user_preferences_id"))
  @OrderColumn(name = "contact_position")
  private List<EmergencyContactEmbeddable> emergencyContacts = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  /** Required by JPA. */
  protected UserPreferencesPersistenceEntity() {}

  /**
   * Constructs a new stored snapshot; subsequent updates preserve its identity and creation time.
   */
  public UserPreferencesPersistenceEntity(
      String id,
      String userId,
      TextSizeLevel textSize,
      boolean highContrast,
      boolean reducedMotion,
      boolean readingAssistance,
      boolean voiceConfirmationEnabled,
      LocalTime quietHoursStart,
      LocalTime quietHoursEnd,
      List<NotificationChannelEmbeddable> channels,
      Instant createdAt) {
    this.id = id;
    this.userId = userId;
    this.textSize = textSize;
    this.highContrast = highContrast;
    this.reducedMotion = reducedMotion;
    this.readingAssistance = readingAssistance;
    this.voiceConfirmationEnabled = voiceConfirmationEnabled;
    this.quietHoursStart = quietHoursStart;
    this.quietHoursEnd = quietHoursEnd;
    this.channels = new ArrayList<>(channels);
    this.createdAt = createdAt;
  }

  /** Returns the stable preference identity. */
  public String getId() {
    return id;
  }

  /** Returns the logical owner reference. */
  public String getUserId() {
    return userId;
  }

  /** Returns the stored text-size choice. */
  public TextSizeLevel getTextSize() {
    return textSize;
  }

  /** Returns the stored high-contrast choice. */
  public boolean isDarkTheme() {
    return Boolean.TRUE.equals(darkTheme);
  }

  public void setDarkTheme(boolean enabled) {
    darkTheme = enabled;
  }

  public boolean isHighContrast() {
    return highContrast;
  }

  /** Returns the stored reduced-motion choice. */
  public boolean isReducedMotion() {
    return reducedMotion;
  }

  /** Returns the stored reading-assistance choice. */
  public boolean isReadingAssistance() {
    return readingAssistance;
  }

  /** Returns the stored voice-confirmation choice. */
  public boolean isVoiceConfirmationEnabled() {
    return voiceConfirmationEnabled;
  }

  /** Returns the nullable daily silence start. */
  public LocalTime getQuietHoursStart() {
    return quietHoursStart;
  }

  /** Returns the nullable daily silence end. */
  public LocalTime getQuietHoursEnd() {
    return quietHoursEnd;
  }

  /** Returns an isolated view of the channel collection. */
  public List<NotificationChannelEmbeddable> getChannels() {
    return List.copyOf(channels);
  }

  /** Returns the original creation evidence. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns stored policy columns, absent for legacy defaults. */
  public NotificationPolicyEmbeddable getNotificationPolicy() {
    return notificationPolicy;
  }

  /** Returns the contacts in their persisted presentation order. */
  public List<EmergencyContactEmbeddable> getEmergencyContacts() {
    return List.copyOf(emergencyContacts);
  }

  /** Copies policy columns for a newly assembled persistence snapshot. */
  public void setNotificationPolicy(NotificationPolicyEmbeddable value) {
    notificationPolicy = value;
  }

  /** Copies contact values without retaining the caller list. */
  public void setEmergencyContacts(List<EmergencyContactEmbeddable> value) {
    emergencyContacts = new ArrayList<>(value);
  }

  /**
   * Records the application-clock instant of a deliberate repository write, including collections.
   */
  public void recordWrite(Instant occurredAt) {
    updatedAt = Objects.requireNonNull(occurredAt, "occurredAt");
  }

  /** Updates only mutable choices on the managed row, preserving identity and audit evidence. */
  public void applyMutableState(UserPreferencesPersistenceEntity source) {
    textSize = source.textSize;
    highContrast = source.highContrast;
    darkTheme = source.darkTheme;
    reducedMotion = source.reducedMotion;
    readingAssistance = source.readingAssistance;
    voiceConfirmationEnabled = source.voiceConfirmationEnabled;
    quietHoursStart = source.quietHoursStart;
    quietHoursEnd = source.quietHoursEnd;
    channels.clear();
    channels.addAll(source.channels);
    notificationPolicy = source.notificationPolicy;
    emergencyContacts.clear();
    emergencyContacts.addAll(source.emergencyContacts);
  }
}
