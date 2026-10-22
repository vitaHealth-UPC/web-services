package com.vitahealth.tata.accessibilitypreferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.vitahealth.tata.accessibilitypreferences.application.commandservices.NotificationPreferencesCommandService;
import com.vitahealth.tata.accessibilitypreferences.application.commandservices.UpdateContrastCommandService;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateContrastCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Verifies expected rejections are captured after rollback while storage failures remain technical.
 */
@SpringBootTest
@ActiveProfiles("test")
class PreferencesCommandOutcomeIntegrationTest {
  @Autowired NotificationPreferencesCommandService notifications;
  @Autowired UpdateContrastCommandService contrast;
  @MockitoSpyBean UserPreferencesRepository repository;

  @Test
  void rejectedDuplicateChannelsReturnFailureAfterRollbackWithoutCreatingDefaults() {
    var user = UUID.randomUUID().toString();
    var outcome =
        notifications.update(
            new UpdateNotificationPreferencesCommand(
                user,
                null,
                List.of(
                    new NotificationChannel(ChannelType.PUSH, true),
                    new NotificationChannel(ChannelType.PUSH, false))));
    assertTrue(outcome instanceof Result.Failure<?, ?>);
    var error = ((Result.Failure<?, ?>) outcome).error();
    assertEquals("VALIDATION_ERROR", ((ApplicationError) error).code());
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    assertTrue(repository.findByUserId(user).isEmpty());
  }

  @Test
  void postFlushIllegalArgumentFromStorageIsNotMisclassifiedAsAValidationFailure() {
    var user = UUID.randomUUID().toString();
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              throw new IllegalArgumentException("simulated technical persistence failure");
            })
        .when(repository)
        .save(any());
    var failure =
        assertThrows(
            RuntimeException.class, () -> contrast.handle(new UpdateContrastCommand(user, true)));
    Throwable origin = failure;
    while (origin.getCause() != null) origin = origin.getCause();
    assertEquals("simulated technical persistence failure", origin.getMessage());
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    assertTrue(repository.findByUserId(user).isEmpty());
  }

  @Test
  void successfulPublicOutcomeIsVisibleAfterItsCommandTransactionCommits() {
    var user = UUID.randomUUID().toString();
    var saved = contrast.handle(new UpdateContrastCommand(user, true)).toOptional().orElseThrow();
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    assertTrue(repository.findByUserId(user).orElseThrow().highContrast());
    assertEquals(user, saved.userId());
  }
}
