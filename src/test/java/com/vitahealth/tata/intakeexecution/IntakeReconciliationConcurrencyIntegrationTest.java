package com.vitahealth.tata.intakeexecution;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.intakeexecution.application.internal.commandservices.GenerateIntakesCommandHandler;
import com.vitahealth.tata.intakeexecution.domain.model.commands.GenerateIntakesCommand;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies reconciliation observes a competing committed confirmation before replacing pending
 * data.
 */
@SpringBootTest
@ActiveProfiles("test")
class IntakeReconciliationConcurrencyIntegrationTest {
  @Autowired GenerateIntakesCommandHandler generation;
  @Autowired IntakeRepository intakes;
  @Autowired PlatformTransactionManager transactions;
  @Autowired Clock clock;

  @Test
  void transitionLookupRefreshesPreviouslyLoadedEvidence() throws Exception {
    var command =
        new GenerateIntakesCommand(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Original",
            "1 comprimido",
            "DAILY",
            List.of(LocalTime.NOON),
            "",
            0,
            true);
    var id = generation.handle(command).getFirst().id();
    var executor = Executors.newSingleThreadExecutor();
    try {
      new TransactionTemplate(transactions)
          .executeWithoutResult(
              status -> {
                assertEquals(IntakeStatus.PENDING, intakes.findById(id).orElseThrow().status());
                try {
                  executor
                      .submit(
                          () ->
                              new TransactionTemplate(transactions)
                                  .executeWithoutResult(
                                      other -> {
                                        var current =
                                            intakes.findByIdForConfirmation(id).orElseThrow();
                                        current.confirm(clock.instant(), ConfirmationChannel.TOUCH);
                                        intakes.saveAll(List.of(current));
                                      }))
                      .get(5, TimeUnit.SECONDS);
                } catch (Exception failure) {
                  throw new AssertionError("competing confirmation did not commit", failure);
                }
                var refreshed = intakes.findByIdForConfirmation(id).orElseThrow();
                assertEquals(IntakeStatus.CONFIRMED, refreshed.status());
                assertEquals(ConfirmationChannel.TOUCH, refreshed.confirmationChannel());
                assertNotNull(refreshed.confirmedAt());
              });
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void transitionLookupRequiresCallerTransaction() {
    assertThrows(
        IllegalTransactionStateException.class,
        () -> intakes.findByIdForConfirmation(UUID.randomUUID().toString()));
  }

  @Test
  void concurrentFirstGenerationSharesOneSetOfPersistentSlotIds() throws Exception {
    var treatment = UUID.randomUUID().toString();
    var command =
        new GenerateIntakesCommand(
            treatment,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Original",
            "1 comprimido",
            "DAILY",
            List.of(LocalTime.NOON),
            "",
            0,
            true);
    var ready = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var first =
          executor.submit(
              () -> {
                ready.await(5, TimeUnit.SECONDS);
                return generation.handle(command);
              });
      var second =
          executor.submit(
              () -> {
                ready.await(5, TimeUnit.SECONDS);
                return generation.handle(command);
              });
      var a = first.get(10, TimeUnit.SECONDS);
      var b = second.get(10, TimeUnit.SECONDS);
      assertEquals(7, a.size());
      assertEquals(
          a.stream().map(value -> value.id()).toList(),
          b.stream().map(value -> value.id()).toList());
      var stored = intakes.findFutureByTreatmentId(treatment, clock.instant());
      assertEquals(7, stored.size());
      assertEquals(7, stored.stream().map(value -> value.scheduledAt()).distinct().count());
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void waitingRegenerationPreservesConfirmedSlotIdentityTimeChannelAndSnapshot() throws Exception {
    var treatment = UUID.randomUUID().toString();
    var medication = UUID.randomUUID().toString();
    var adult = UUID.randomUUID().toString();
    var initial =
        new GenerateIntakesCommand(
            treatment,
            medication,
            adult,
            "Original",
            "1 comprimido",
            "DAILY",
            List.of(LocalTime.NOON),
            "Original instructions",
            0,
            true);
    var generated = generation.handle(initial);
    var first = generated.getFirst();
    var held = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var started = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var confirmation =
          executor.submit(
              () ->
                  new TransactionTemplate(transactions)
                      .execute(
                          status -> {
                            var intake = intakes.findByIdForConfirmation(first.id()).orElseThrow();
                            intake.confirm(clock.instant(), ConfirmationChannel.TOUCH);
                            intakes.saveAll(List.of(intake));
                            held.countDown();
                            try {
                              if (!release.await(5, TimeUnit.SECONDS))
                                throw new IllegalStateException(
                                    "test confirmation barrier timed out");
                            } catch (InterruptedException failure) {
                              Thread.currentThread().interrupt();
                              throw new IllegalStateException(failure);
                            }
                            return intake.confirmedAt();
                          }));
      assertTrue(held.await(5, TimeUnit.SECONDS));
      var replay =
          executor.submit(
              () -> {
                started.countDown();
                return generation.handle(
                    new GenerateIntakesCommand(
                        treatment,
                        medication,
                        adult,
                        "Updated",
                        "2 comprimidos",
                        "DAILY",
                        List.of(LocalTime.NOON),
                        "Updated instructions",
                        0,
                        true));
              });
      assertTrue(started.await(5, TimeUnit.SECONDS));
      assertThrows(TimeoutException.class, () -> replay.get(150, TimeUnit.MILLISECONDS));
      release.countDown();
      var confirmedAt = confirmation.get(5, TimeUnit.SECONDS);
      var pending = replay.get(5, TimeUnit.SECONDS);
      assertFalse(pending.stream().anyMatch(value -> value.id().equals(first.id())));
      var stored = intakes.findById(first.id()).orElseThrow();
      assertEquals(IntakeStatus.CONFIRMED, stored.status());
      assertEquals(first.scheduledAt(), stored.scheduledAt());
      assertEquals(confirmedAt, stored.confirmedAt());
      assertEquals(ConfirmationChannel.TOUCH, stored.confirmationChannel());
      assertEquals("Original", stored.medication().name());
      assertEquals(generated.size() - 1, pending.size());
      assertEquals(
          generated.stream().skip(1).map(value -> value.id()).toList(),
          pending.stream().map(value -> value.id()).toList());
    } finally {
      release.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  /** Keeps all seven daily noon slots future regardless of when this concurrency test is run. */
  @TestConfiguration
  static class ScheduleTime {
    @Bean
    @Primary
    Clock reconciliationClock() {
      return Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"), ZoneOffset.UTC);
    }
  }
}
