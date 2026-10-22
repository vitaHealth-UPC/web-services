package com.vitahealth.tata.accessibilitypreferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateContrastCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateVoiceConfirmationCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.internal.commandservices.UserPreferencesMutationService;
import com.vitahealth.tata.accessibilitypreferences.application.internal.outboundservices.PreferencesMutationLock;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateContrastCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateVoiceConfirmationCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exercises absent-state serialization and fresh existing-state updates across real transactions.
 */
@SpringBootTest
@ActiveProfiles("test")
class PreferencesMutationConcurrencyIntegrationTest {
  @Autowired UserPreferencesRepository preferences;
  @Autowired PreferencesMutationLock reservations;
  @Autowired UserPreferencesMutationService mutations;
  @Autowired UpdateContrastCommandService contrast;
  @Autowired UpdateVoiceConfirmationCommandService voice;
  @Autowired PlatformTransactionManager transactions;
  @Autowired JdbcTemplate jdbc;

  @Test
  void mutationCapabilitiesRequireTheCallersTransaction() {
    var user = UUID.randomUUID().toString();
    assertThrows(IllegalTransactionStateException.class, () -> reservations.reserve(user));
    assertThrows(IllegalTransactionStateException.class, () -> mutations.load(user));
    assertThrows(
        IllegalTransactionStateException.class, () -> preferences.findByUserIdForUpdate(user));
  }

  @Test
  void simultaneousFirstCreationPreservesBothDecisionsAndOnePreferenceRow() throws Exception {
    var user = UUID.randomUUID().toString();
    var start = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first =
          workers.submit(
              () -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return contrast
                    .handle(new UpdateContrastCommand(user, true))
                    .toOptional()
                    .orElseThrow();
              });
      var second =
          workers.submit(
              () -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return voice
                    .handle(new UpdateVoiceConfirmationCommand(user, false))
                    .toOptional()
                    .orElseThrow();
              });
      start.countDown();
      assertEquals(first.get(15, TimeUnit.SECONDS).id(), second.get(15, TimeUnit.SECONDS).id());
    }
    var stored = preferences.findByUserId(user).orElseThrow();
    assertTrue(stored.highContrast());
    assertFalse(stored.voiceConfirmationEnabled());
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from user_preferences where user_id = ?", Integer.class, user));
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from user_preference_locks where user_id = ?", Integer.class, user));
  }

  @Test
  void reservedFirstMutationBlocksTheSecondUntilItsCompleteStateCommits() throws Exception {
    var user = UUID.randomUUID().toString();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var started = new CountDownLatch(1);
    var tx = new TransactionTemplate(transactions);
    try (var workers = Executors.newFixedThreadPool(2)) {
      try {
        var first =
            workers.submit(
                () ->
                    tx.execute(
                        status -> {
                          var state = mutations.load(user);
                          state.setHighContrast(true);
                          preferences.save(state);
                          entered.countDown();
                          try {
                            assertTrue(release.await(10, TimeUnit.SECONDS));
                          } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(interrupted);
                          }
                          return state.id();
                        }));
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        var second =
            workers.submit(
                () -> {
                  started.countDown();
                  return voice
                      .handle(new UpdateVoiceConfirmationCommand(user, false))
                      .toOptional()
                      .orElseThrow();
                });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
        release.countDown();
        assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS).id());
      } finally {
        release.countDown();
      }
    }
    var stored = preferences.findByUserId(user).orElseThrow();
    assertTrue(stored.highContrast());
    assertFalse(stored.voiceConfirmationEnabled());
  }

  @Test
  void lockedReloadRefreshesAStateReadBeforeAnotherTransactionCommitted() throws Exception {
    var user = UUID.randomUUID().toString();
    contrast.handle(new UpdateContrastCommand(user, false)).toOptional().orElseThrow();
    try (var workers = Executors.newSingleThreadExecutor()) {
      new TransactionTemplate(transactions)
          .executeWithoutResult(
              status -> {
                assertFalse(preferences.findByUserId(user).orElseThrow().highContrast());
                try {
                  workers
                      .submit(
                          () ->
                              contrast
                                  .handle(new UpdateContrastCommand(user, true))
                                  .toOptional()
                                  .orElseThrow())
                      .get(10, TimeUnit.SECONDS);
                } catch (Exception failure) {
                  throw new IllegalStateException(failure);
                }
                var refreshed = mutations.load(user);
                assertTrue(refreshed.highContrast());
                refreshed.setVoiceConfirmation(false);
                preferences.save(refreshed);
              });
    }
    assertTrue(preferences.findByUserId(user).orElseThrow().highContrast());
    assertFalse(preferences.findByUserId(user).orElseThrow().voiceConfirmationEnabled());
  }

  @Test
  void rollbackRetainsOnlyTheTechnicalKeyAndAnotherUserCanProgress() {
    var rolledBack = UUID.randomUUID().toString();
    var independent = UUID.randomUUID().toString();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              contrast
                  .handle(new UpdateContrastCommand(rolledBack, true))
                  .toOptional()
                  .orElseThrow();
              status.setRollbackOnly();
            });
    assertTrue(preferences.findByUserId(rolledBack).isEmpty());
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from user_preference_locks where user_id = ?",
            Integer.class,
            rolledBack));
    contrast.handle(new UpdateContrastCommand(independent, true)).toOptional().orElseThrow();
    assertTrue(preferences.findByUserId(independent).orElseThrow().highContrast());
    assertFalse(
        voice
            .handle(new UpdateVoiceConfirmationCommand(rolledBack, false))
            .toOptional()
            .orElseThrow()
            .voiceConfirmationEnabled());
  }
}
