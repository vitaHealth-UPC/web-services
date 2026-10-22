package com.vitahealth.tata.intakeexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.commandservices.ConfirmIntakeCommandService;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.events.IntakeConfirmedEventPublisher;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

/**
 * Verifies public confirmation outcomes are captured after the confirming transaction rolls back.
 */
@SpringBootTest
@ActiveProfiles("test")
class IntakeConfirmationTransactionIntegrationTest {
  @Autowired ConfirmIntakeCommandService confirmation;
  @Autowired IntakeRepository repository;
  @Autowired IntakeConfirmedEventPublisher publisher;
  @Autowired FailingConsumer consumer;

  @Test
  void expectedConsumerRejectionBecomesFailureAfterSourceRollback() {
    var intake = pending();
    consumer.target.set(intake.id());
    consumer.observedConfirmation = false;
    consumer.failure =
        new IntakeApplicationException(
            IntakeApplicationException.Code.INTAKE_NOT_CONFIRMABLE,
            "dependent confirmation rejected");
    try {
      var outcome =
          confirmation.handle(new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.TOUCH));
      var failure = assertInstanceOf(Result.Failure.class, outcome);
      assertEquals("INTAKE_NOT_CONFIRMABLE", ((ApplicationError) failure.error()).code());
      assertTrue(consumer.observedConfirmation);
      assertPending(intake.id());
    } finally {
      consumer.target.set(null);
    }
  }

  @Test
  void technicalConsumerFailurePropagatesAfterSourceRollback() {
    var intake = pending();
    consumer.target.set(intake.id());
    consumer.observedConfirmation = false;
    consumer.failure = new IllegalStateException("simulated dependent storage failure");
    try {
      assertThrows(
          IllegalStateException.class,
          () ->
              confirmation.handle(
                  new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.TOUCH)));
      assertTrue(consumer.observedConfirmation);
      assertPending(intake.id());
    } finally {
      consumer.target.set(null);
    }
  }

  @Test
  void synchronousEventDeliveryRequiresTheCallerTransaction() {
    assertThrows(
        IllegalTransactionStateException.class,
        () ->
            publisher.publish(new IntakeConfirmed("intake", "medication", "adult", Instant.now())));
  }

  private Intake pending() {
    return repository
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
  }

  private void assertPending(String id) {
    var stored = repository.findById(id).orElseThrow();
    assertEquals(IntakeStatus.PENDING, stored.status());
    assertNull(stored.confirmedAt());
    assertNull(stored.confirmationChannel());
  }

  /** Fails only the selected confirmation after its state is visible inside the transaction. */
  static class FailingConsumer {
    final AtomicReference<String> target = new AtomicReference<>();
    final IntakeRepository repository;
    RuntimeException failure;
    boolean observedConfirmation;

    FailingConsumer(IntakeRepository repository) {
      this.repository = repository;
    }

    @EventListener
    @Order(1000)
    void on(IntakeConfirmed event) {
      if (event.intakeId().equals(target.get())) {
        observedConfirmation = !repository.findById(event.intakeId()).orElseThrow().isPending();
        throw failure;
      }
    }
  }

  /** Installs the selectively failing consumer only for this integration context. */
  @TestConfiguration
  static class Consumers {
    @Bean
    FailingConsumer failingConsumer(IntakeRepository repository) {
      return new FailingConsumer(repository);
    }
  }
}
