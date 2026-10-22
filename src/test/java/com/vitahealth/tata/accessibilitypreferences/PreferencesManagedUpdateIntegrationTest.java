package com.vitahealth.tata.accessibilitypreferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.vitahealth.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.repositories.UserPreferencesJpaRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Verifies managed writes preserve stored identity and audit data instead of merging replacements.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PreferencesManagedUpdateIntegrationTest {
  @Autowired UserPreferencesRepository repository;
  @Autowired EntityManager entities;
  @Autowired JdbcTemplate jdbc;
  @Autowired UserPreferencesJpaRepository rows;

  @Test
  void incomingCreationTimeCannotReplaceOriginalButMutableChoicesAndContactOrderAreSaved() {
    var original =
        repository.save(
            UserPreferencesFactory.createDefaults(UUID.randomUUID().toString(), Instant.EPOCH));
    var replacement = copy(original, original.id(), Instant.parse("2026-10-15T12:00:00Z"));
    replacement.setHighContrast(true);
    var contacts =
        List.of(
            new EmergencyContact("Rosa", "+51999123456"),
            new EmergencyContact("Diego", "+51988123456"));
    replacement.updateNotificationPolicy(NotificationPolicy.defaults(), contacts);

    var saved = repository.save(replacement);
    assertEquals(Instant.EPOCH, saved.createdAt());
    assertEquals(original.id(), saved.id());
    entities.clear();
    var reloaded = repository.findByUserId(original.userId()).orElseThrow();
    assertEquals(Instant.EPOCH, reloaded.createdAt());
    assertTrue(reloaded.highContrast());
    assertEquals(contacts, reloaded.emergencyContacts());
    assertEquals(original.notificationChannels(), reloaded.notificationChannels());
  }

  @Test
  void differentIdentityForTheSameOwnerIsRejectedBeforeChangingPreferences() {
    var original =
        repository.save(
            UserPreferencesFactory.createDefaults(UUID.randomUUID().toString(), Instant.EPOCH));
    var replacement = copy(original, UUID.randomUUID().toString(), Instant.EPOCH);
    replacement.setHighContrast(true);
    assertThrows(RuntimeException.class, () -> repository.save(replacement));
    entities.clear();
    var stored = repository.findByUserId(original.userId()).orElseThrow();
    assertEquals(original.id(), stored.id());
    assertEquals(original.highContrast(), stored.highContrast());
  }

  @Test
  void contactOnlyWriteRenewsAuditTimeAndSubsequentReadPreservesIt() {
    var original =
        repository.save(
            UserPreferencesFactory.createDefaults(UUID.randomUUID().toString(), Instant.EPOCH));
    jdbc.update(
        "update user_preferences set updated_at = ? where id = ?",
        Timestamp.from(Instant.EPOCH),
        original.id());
    entities.clear();
    var replacement = repository.findByUserId(original.userId()).orElseThrow();
    replacement.updateNotificationPolicy(
        replacement.notificationPolicy(), List.of(new EmergencyContact("Rosa", "+51999123456")));
    repository.save(replacement);
    entities.clear();
    var writtenAt =
        jdbc.queryForObject(
            "select updated_at from user_preferences where id = ?", Timestamp.class, original.id());
    assertTrue(writtenAt.toInstant().isAfter(Instant.EPOCH));
    repository.findByUserId(original.userId()).orElseThrow();
    entities.flush();
    assertEquals(
        writtenAt,
        jdbc.queryForObject(
            "select updated_at from user_preferences where id = ?",
            Timestamp.class,
            original.id()));
    assertEquals(
        Instant.EPOCH, repository.findByUserId(original.userId()).orElseThrow().createdAt());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void repositoryReadReturnsCompleteCollectionsWithoutAnAmbientTransactionOrOpenSession() {
    var source = UserPreferencesFactory.createDefaults(UUID.randomUUID().toString(), Instant.EPOCH);
    var contacts =
        List.of(
            new EmergencyContact("Rosa", "+51999123456"),
            new EmergencyContact("Diego", "+51988123456"));
    source.updateNotificationPolicy(source.notificationPolicy(), contacts);
    try {
      repository.save(source);
      var loaded = repository.findByUserId(source.userId()).orElseThrow();
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
      assertEquals(contacts, loaded.emergencyContacts());
      assertEquals(source.notificationChannels(), loaded.notificationChannels());
    } finally {
      rows.deleteById(source.id());
    }
  }

  private static UserPreferences copy(UserPreferences source, String id, Instant created) {
    return UserPreferences.rehydrate(
        id,
        source.userId(),
        source.textSize(),
        source.highContrast(),
        source.reducedMotion(),
        source.readingAssistance(),
        source.voiceConfirmationEnabled(),
        source.quietHours(),
        source.notificationChannels(),
        created,
        source.notificationPolicy(),
        source.emergencyContacts());
  }
}
