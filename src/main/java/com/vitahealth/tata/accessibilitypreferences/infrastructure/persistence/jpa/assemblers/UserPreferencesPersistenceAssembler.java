package com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.QuietHoursRange;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables.EmergencyContactEmbeddable;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables.NotificationChannelEmbeddable;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables.NotificationPolicyEmbeddable;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities.UserPreferencesPersistenceEntity;

/**
 * Converts UserPreferences state between domain and JPA representations without database access.
 */
public final class UserPreferencesPersistenceAssembler {
  private UserPreferencesPersistenceAssembler() {}

  /**
   * Restores the stored identity and state without executing a business transition.
   *
   * @param entity stored JPA representation
   * @return reconstructed domain object
   * @throws IllegalStateException if saved evidence is incomplete or violates domain constraints
   */
  public static UserPreferences toDomain(UserPreferencesPersistenceEntity entity) {
    try {
      return restore(entity);
    } catch (IllegalArgumentException | NullPointerException invalidState) {
      throw new IllegalStateException("stored user preferences are inconsistent", invalidState);
    }
  }

  private static UserPreferences restore(UserPreferencesPersistenceEntity entity) {
    if ((entity.getQuietHoursStart() == null) != (entity.getQuietHoursEnd() == null))
      throw new IllegalArgumentException("stored quiet hours are incomplete");
    QuietHoursRange quietHours =
        entity.getQuietHoursStart() == null || entity.getQuietHoursEnd() == null
            ? null
            : QuietHoursRange.between(entity.getQuietHoursStart(), entity.getQuietHoursEnd());
    return UserPreferences.rehydrate(
        entity.getId(),
        entity.getUserId(),
        entity.getTextSize(),
        entity.isHighContrast(),
        entity.isReducedMotion(),
        entity.isReadingAssistance(),
        entity.isVoiceConfirmationEnabled(),
        quietHours,
        entity.getChannels().stream()
            .map(c -> new NotificationChannel(c.getType(), c.isEnabled()))
            .toList(),
        entity.getCreatedAt(),
        entity.getNotificationPolicy() == null
            ? NotificationPolicy.defaults()
            : policyFrom(entity.getNotificationPolicy()),
        entity.getEmergencyContacts().stream()
            .map(c -> new EmergencyContact(c.getName(), c.getPhone()))
            .toList());
  }

  /**
   * Builds the persistence representation while preserving the domain identity.
   *
   * @return JPA state ready for the repository adapter
   */
  public static UserPreferencesPersistenceEntity toEntity(UserPreferences preferences) {
    var quietHours = preferences.quietHours();
    var entity =
        new UserPreferencesPersistenceEntity(
            preferences.id(),
            preferences.userId(),
            preferences.textSize(),
            preferences.highContrast(),
            preferences.reducedMotion(),
            preferences.readingAssistance(),
            preferences.voiceConfirmationEnabled(),
            quietHours == null ? null : quietHours.start(),
            quietHours == null ? null : quietHours.end(),
            preferences.notificationChannels().stream()
                .map(c -> new NotificationChannelEmbeddable(c.type(), c.enabled()))
                .toList(),
            preferences.createdAt());
    var policy = preferences.notificationPolicy();
    entity.setNotificationPolicy(
        new NotificationPolicyEmbeddable(
            policy.reminderMinutes(),
            policy.pendingAlertMinutes(),
            policy.omittedAlertMinutes(),
            policy.escalationMinutes(),
            policy.dailySummaryTime()));
    entity.setEmergencyContacts(
        preferences.emergencyContacts().stream()
            .map(c -> new EmergencyContactEmbeddable(c.name(), c.phone()))
            .toList());
    return entity;
  }

  private static NotificationPolicy policyFrom(NotificationPolicyEmbeddable value) {
    if (value.getReminderMinutes() == null
        && value.getPendingAlertMinutes() == null
        && value.getOmittedAlertMinutes() == null
        && value.getEscalationMinutes() == null
        && value.getDailySummaryTime() == null) return NotificationPolicy.defaults();
    if (value.getReminderMinutes() == null
        || value.getPendingAlertMinutes() == null
        || value.getOmittedAlertMinutes() == null
        || value.getEscalationMinutes() == null)
      throw new IllegalArgumentException("stored notification policy is incomplete");
    return new NotificationPolicy(
        value.getReminderMinutes(),
        value.getPendingAlertMinutes(),
        value.getOmittedAlertMinutes(),
        value.getEscalationMinutes(),
        value.getDailySummaryTime());
  }
}
