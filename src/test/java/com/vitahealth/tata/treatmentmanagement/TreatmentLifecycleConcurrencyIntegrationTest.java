package com.vitahealth.tata.treatmentmanagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import com.vitahealth.tata.treatmentmanagement.application.commandservices.TreatmentCommandService;
import com.vitahealth.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.vitahealth.tata.treatmentmanagement.application.internal.commandservices.ExtendTreatmentScheduleCommandHandler;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.CareLinkVerificationPort;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.events.TreatmentScheduleEventPublisher;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.*;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Exercises competing committed handler transactions with deterministic publication barriers. */
@SpringBootTest
@ActiveProfiles("test")
class TreatmentLifecycleConcurrencyIntegrationTest {
  @Autowired TreatmentCommandService commands;
  @Autowired ExtendTreatmentScheduleCommandHandler extension;
  @Autowired MedicationRepository medications;
  @Autowired TreatmentRepository treatments;
  @MockitoBean CareLinkVerificationPort links;
  @MockitoBean TreatmentScheduleEventPublisher publication;

  @Test
  void activationFinishesBeforeWaitingDeactivationPausesItsTreatment() throws Exception {
    compete(true);
  }

  @Test
  void committedDeactivationRejectsWaitingActivation() throws Exception {
    compete(false);
  }

  @Test
  void extensionWaitingForPauseDiscardsItsEarlierActiveSnapshot() throws Exception {
    when(links.isAuthorized(anyString(), anyString())).thenReturn(true);
    var caregiver = UUID.randomUUID().toString();
    var adult = UUID.randomUUID().toString();
    var medication =
        commands
            .registerMedication(
                new RegisterMedicationCommand(caregiver, adult, "Losartán", "Comprimido"))
            .toOptional()
            .orElseThrow();
    var treatment =
        commands
            .createTreatment(new CreateTreatmentCommand(caregiver, adult, "Presión"))
            .toOptional()
            .orElseThrow();
    commands
        .configureTreatment(
            new ConfigureTreatmentCommand(
                caregiver,
                treatment.id(),
                medication.id(),
                "1 comprimido",
                "DAILY",
                List.of(LocalTime.NOON),
                "",
                0))
        .toOptional()
        .orElseThrow();
    commands
        .activate(new ChangeTreatmentStatusCommand(caregiver, treatment.id()))
        .toOptional()
        .orElseThrow();
    clearInvocations(publication);
    var held = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var started = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              var event = invocation.getArgument(0, TreatmentScheduleChangedEvent.class);
              if (event.treatmentId().equals(treatment.id()) && !event.active()) {
                held.countDown();
                if (!release.await(5, TimeUnit.SECONDS))
                  throw new IllegalStateException("test pause barrier timed out");
              }
              return null;
            })
        .when(publication)
        .publish(any());
    var executor = Executors.newFixedThreadPool(2);
    try {
      var pause =
          executor.submit(
              () -> commands.pause(new ChangeTreatmentStatusCommand(caregiver, treatment.id())));
      assertTrue(held.await(5, TimeUnit.SECONDS));
      var pending =
          executor.submit(
              () -> {
                started.countDown();
                return extension.handle(new ExtendTreatmentScheduleCommand(treatment.id()));
              });
      assertTrue(started.await(5, TimeUnit.SECONDS));
      assertThrows(TimeoutException.class, () -> pending.get(150, TimeUnit.MILLISECONDS));
      release.countDown();
      assertTrue(pause.get(5, TimeUnit.SECONDS).isSuccess());
      assertFalse(pending.get(5, TimeUnit.SECONDS));
      assertEquals(
          TreatmentStatus.PAUSED, treatments.findById(treatment.id()).orElseThrow().status());
      verify(publication, times(1)).publish(any());
    } finally {
      release.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private void compete(boolean activationFirst) throws Exception {
    when(links.isAuthorized(anyString(), anyString())).thenReturn(true);
    var caregiver = UUID.randomUUID().toString();
    var adult = UUID.randomUUID().toString();
    var medication =
        commands
            .registerMedication(
                new RegisterMedicationCommand(caregiver, adult, "Losartán", "Comprimido"))
            .toOptional()
            .orElseThrow();
    var treatment =
        commands
            .createTreatment(new CreateTreatmentCommand(caregiver, adult, "Presión"))
            .toOptional()
            .orElseThrow();
    commands
        .configureTreatment(
            new ConfigureTreatmentCommand(
                caregiver,
                treatment.id(),
                medication.id(),
                "1 comprimido",
                "DAILY",
                List.of(LocalTime.NOON),
                "",
                0))
        .toOptional()
        .orElseThrow();
    if (!activationFirst)
      commands
          .activate(new ChangeTreatmentStatusCommand(caregiver, treatment.id()))
          .toOptional()
          .orElseThrow();
    var held = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var competing = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              var event = invocation.getArgument(0, TreatmentScheduleChangedEvent.class);
              if (event.treatmentId().equals(treatment.id()) && event.active() == activationFirst) {
                held.countDown();
                if (!release.await(5, TimeUnit.SECONDS))
                  throw new IllegalStateException("test publication barrier timed out");
              }
              return null;
            })
        .when(publication)
        .publish(any());
    var executor = Executors.newFixedThreadPool(2);
    try {
      var first =
          executor.submit(
              () ->
                  activationFirst
                      ? commands.activate(
                          new ChangeTreatmentStatusCommand(caregiver, treatment.id()))
                      : commands.deactivateMedication(
                          new DeactivateMedicationCommand(caregiver, medication.id())));
      assertTrue(held.await(5, TimeUnit.SECONDS));
      var second =
          executor.submit(
              () -> {
                competing.countDown();
                return activationFirst
                    ? commands.deactivateMedication(
                        new DeactivateMedicationCommand(caregiver, medication.id()))
                    : commands.activate(
                        new ChangeTreatmentStatusCommand(caregiver, treatment.id()));
              });
      assertTrue(competing.await(5, TimeUnit.SECONDS));
      assertThrows(TimeoutException.class, () -> second.get(150, TimeUnit.MILLISECONDS));
      release.countDown();
      assertTrue(first.get(5, TimeUnit.SECONDS).isSuccess());
      var outcome = second.get(5, TimeUnit.SECONDS);
      if (activationFirst) assertTrue(outcome.isSuccess());
      else {
        assertInstanceOf(Result.Failure.class, outcome);
        assertEquals(
            "MEDICATION_INACTIVE",
            ((ApplicationError) ((Result.Failure<?, ?>) outcome).error()).code());
      }
      assertFalse(medications.findById(medication.id()).orElseThrow().active());
      assertEquals(
          TreatmentStatus.PAUSED, treatments.findById(treatment.id()).orElseThrow().status());
    } finally {
      release.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }
}
