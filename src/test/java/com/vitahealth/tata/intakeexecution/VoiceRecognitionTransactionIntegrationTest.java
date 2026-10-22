package com.vitahealth.tata.intakeexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.intakeexecution.application.commandservices.ConfirmIntakeByVoiceCommandService;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoicePreferencePort;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoiceRecognitionPort;
import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.application.result.VoiceConfirmationResult.VoiceConfirmationStatus;
import com.vitahealth.tata.intakeexecution.application.result.VoiceRecognitionResult;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeByVoiceCommand;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.VoiceConfirmationLanguage;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.support.WavAudioFixture;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies provider work does not inherit a caller's ambient database transaction. */
@SpringBootTest
@ActiveProfiles("test")
class VoiceRecognitionTransactionIntegrationTest {
  @Autowired ConfirmIntakeByVoiceCommandService service;
  @Autowired IntakeRepository repository;
  @Autowired PlatformTransactionManager transactions;

  @MockitoBean(enforceOverride = true)
  VoiceRecognitionPort recognition;

  @MockitoBean(enforceOverride = true)
  VoicePreferencePort preferences;

  @Test
  void suspendsAmbientTransactionForRecognitionAndRestoresTheCaller() {
    var intake =
        repository
            .saveAll(
                List.of(
                    Intake.createScheduled(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        new MedicationSnapshot("Losartán", "1 comprimido", ""),
                        Instant.now().plusSeconds(60),
                        Instant.now())))
            .getFirst();
    when(preferences.isVoiceConfirmationEnabled(anyString())).thenReturn(true);
    var calls = new AtomicInteger();
    when(recognition.recognize(
            any(ValidatedVoiceAudio.class), any(VoiceConfirmationLanguage.class)))
        .thenAnswer(
            invocation -> {
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              calls.incrementAndGet();
              return VoiceRecognitionResult.unrecognized(null, 0);
            });
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            transaction -> {
              assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
              var result =
                  service
                      .handle(
                          new ConfirmIntakeByVoiceCommand(
                              intake.id(), WavAudioFixture.shortRecording(), "audio/wav", "es-419"))
                      .toOptional()
                      .orElseThrow();
              assertEquals(VoiceConfirmationStatus.NOT_RECOGNIZED, result.status());
              assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            });
    assertEquals(1, calls.get());
  }
}
