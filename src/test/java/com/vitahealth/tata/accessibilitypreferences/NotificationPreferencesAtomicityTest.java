package com.vitahealth.tata.accessibilitypreferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.vitahealth.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.NotificationPreferencesController;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.ChannelResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.QuietHoursResource;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateQuietHoursResource;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Forces failure after a real flush to prove combined replacements never persist partial state. */
@SpringBootTest
@ActiveProfiles("test")
class NotificationPreferencesAtomicityTest {
  @Autowired NotificationPreferencesController controller;
  @MockitoSpyBean UserPreferencesRepository repository;
  @Autowired JdbcTemplate jdbc;

  @Test
  void failureAfterFlushingNewPreferencesRollsBackTheCompleteCreation() {
    var user = UUID.randomUUID().toString();
    var observed = failAfterRealFlush(user);
    var failure =
        assertThrows(
            RuntimeException.class,
            () -> controller.update(user, new UpdateQuietHoursResource(null, List.of())));
    assertEquals("simulated post-flush failure", failure.getMessage());
    assertTrue(observed.get());
    assertTrue(repository.findByUserId(user).isEmpty());
  }

  @Test
  void failureAfterFlushingAnExistingReplacementPreservesEveryPreviousChoice() {
    var user = UUID.randomUUID().toString();
    var original = UserPreferencesFactory.createDefaults(user, Instant.EPOCH);
    original.setHighContrast(true);
    original.setVoiceConfirmation(false);
    repository.save(original);
    var observed = failAfterRealFlush(user);
    var replacement =
        new UpdateQuietHoursResource(
            new QuietHoursResource(LocalTime.of(22, 0), LocalTime.of(7, 0)),
            List.of(new ChannelResource(ChannelType.EMAIL, true)));

    assertThrows(RuntimeException.class, () -> controller.update(user, replacement));
    assertTrue(observed.get());
    var restored = repository.findByUserId(user).orElseThrow();
    assertEquals(original.id(), restored.id());
    assertEquals(Instant.EPOCH, restored.createdAt());
    assertTrue(restored.highContrast());
    assertFalse(restored.voiceConfirmationEnabled());
    assertNull(restored.quietHours());
    assertTrue(restored.isChannelEnabled(ChannelType.PUSH));
    assertFalse(restored.isChannelEnabled(ChannelType.EMAIL));
  }

  private AtomicBoolean failAfterRealFlush(String user) {
    var observed = new AtomicBoolean();
    doAnswer(
            call -> {
              call.callRealMethod();
              assertEquals(
                  1,
                  jdbc.queryForObject(
                      "select count(*) from user_preferences where user_id = ?",
                      Integer.class,
                      user));
              observed.set(true);
              throw new IllegalStateException("simulated post-flush failure");
            })
        .when(repository)
        .save(any());
    return observed;
  }
}
