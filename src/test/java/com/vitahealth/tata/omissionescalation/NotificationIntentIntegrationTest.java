package com.vitahealth.tata.omissionescalation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationIntentStore;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentReceipt;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentRequest;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationIntentState;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Exercises actual commit, rollback, unique source ownership and recoverable fanout leases. */
@SpringBootTest
@ActiveProfiles("test")
class NotificationIntentIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
  @Autowired NotificationIntentStore queue;
  @Autowired NotificationIntentJpaRepository intents;
  @Autowired OmissionCaseJpaRepository caseRows;
  @Autowired IOmissionCaseRepository cases;
  @Autowired PlatformTransactionManager manager;
  @Autowired JdbcTemplate jdbc;
  private TransactionTemplate transaction;
  private final List<Long> sources = new ArrayList<>();
  private final List<String> intentIds = new ArrayList<>();

  @BeforeEach
  void setup() {
    transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @AfterEach
  void cleanup() {
    transaction.executeWithoutResult(
        status -> {
          intents.deleteAllById(intentIds);
          intents.flush();
          caseRows.deleteAllById(sources);
        });
  }

  private long source() {
    var saved =
        cases.save(
            new OmissionCase(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "Medication",
                NOW,
                GracePeriod.startingAt(NOW, Duration.ofMinutes(30))));
    sources.add(saved.getId());
    return saved.getId();
  }

  private NotificationIntentReceipt enqueue(long source) {
    var result =
        queue.enqueue(
            new NotificationIntentRequest(source, NotificationKind.REINFORCED_REMINDER, NOW));
    intentIds.add(result.intentId());
    return result;
  }

  @Test
  void cannotEnqueueOutsideTheBusinessTransaction() {
    assertThatThrownBy(
            () ->
                queue.enqueue(
                    new NotificationIntentRequest(1, NotificationKind.REINFORCED_REMINDER, NOW)))
        .isInstanceOf(IllegalTransactionStateException.class);
  }

  @Test
  void sourceAndIntentRollBackTogether() {
    var result =
        transaction.execute(
            status -> {
              long source = source();
              var receipt = enqueue(source);
              status.setRollbackOnly();
              return receipt;
            });
    assertThat(intents.findById(result.intentId())).isEmpty();
    assertThat(caseRows.findById(sources.getFirst())).isEmpty();
  }

  @Test
  void repeatRetainsOriginalIntentAndDoesNotMarkSourceSent() {
    var original =
        transaction.execute(
            status -> {
              long source = source();
              return enqueue(source);
            });
    long source = sources.getFirst();
    var repeated =
        transaction.execute(
            status ->
                queue.enqueue(
                    new NotificationIntentRequest(
                        source, NotificationKind.REINFORCED_REMINDER, NOW.plusSeconds(10))));
    assertThat(repeated).isEqualTo(original);
    var distinct =
        transaction.execute(
            status ->
                queue.enqueue(
                    new NotificationIntentRequest(source, NotificationKind.CAREGIVER_ALERT, NOW)));
    intentIds.add(distinct.intentId());
    assertThat(distinct.intentId()).isNotEqualTo(original.intentId());
    var caseState = cases.findById(source).orElseThrow();
    assertThat(caseState.getReinforcedReminderSentAt()).isNull();
    assertThat(caseState.getAlerts()).isEmpty();
  }

  @Test
  void uncommittedIntentionIsInvisibleUntilSourceTransactionCommits() {
    var receipt =
        transaction.execute(
            status -> {
              var queued = enqueue(source());
              transaction.executeWithoutResult(
                  observer -> assertThat(intents.findById(queued.intentId())).isEmpty());
              return queued;
            });
    assertThat(intents.findById(receipt.intentId())).isPresent();
  }

  @Test
  void expiredAndReplacedWorkersCannotOverwriteCommittedFanoutProgress() {
    var receipt = transaction.execute(status -> enqueue(source()));
    transaction.executeWithoutResult(
        status ->
            assertThat(
                    intents
                        .findForUpdate(receipt.intentId())
                        .orElseThrow()
                        .claimFanout("worker-1", NOW, Duration.ofSeconds(30)))
                .isTrue());
    transaction.executeWithoutResult(
        status -> {
          var intent = intents.findForUpdate(receipt.intentId()).orElseThrow();
          assertThat(intent.claimFanout("worker-2", NOW.plusSeconds(29), Duration.ofSeconds(30)))
              .isFalse();
          assertThat(intent.completeFanout("worker-1", NOW.plusSeconds(30))).isFalse();
          assertThat(intent.claimFanout("worker-2", NOW.plusSeconds(30), Duration.ofSeconds(30)))
              .isTrue();
          assertThat(
                  intent.advanceFanout("worker-1", NOW.plusSeconds(31), "stale", "stale", "stale"))
              .isFalse();
          assertThat(
                  intent.advanceFanout(
                      "worker-2", NOW.plusSeconds(31), "caregiver-a", "caregiver-b", "device-a"))
              .isTrue();
          assertThat(
                  intent.rescheduleFanout("worker-2", NOW.plusSeconds(31), Duration.ofSeconds(10)))
              .isTrue();
        });
    var saved = intents.findById(receipt.intentId()).orElseThrow();
    assertThat(saved.getCaregiverAfterId()).isEqualTo("caregiver-a");
    assertThat(saved.getCurrentCaregiverId()).isEqualTo("caregiver-b");
    assertThat(saved.getDeviceAfterId()).isEqualTo("device-a");
    assertThat(saved.getLeaseUntil()).isNull();
    assertThat(
            intents.findDueIds(
                NotificationIntentState.READY, NOW.plusSeconds(40), PageRequest.of(0, 20)))
        .doesNotContain(receipt.intentId());
    assertThat(
            intents.findDueIds(
                NotificationIntentState.READY, NOW.plusSeconds(41), PageRequest.of(0, 20)))
        .contains(receipt.intentId());
    transaction.executeWithoutResult(
        status -> {
          var intent = intents.findForUpdate(receipt.intentId()).orElseThrow();
          assertThat(intent.claimFanout("worker-3", NOW.plusSeconds(41), Duration.ofSeconds(30)))
              .isTrue();
          assertThat(intent.completeFanout("worker-3", NOW.plusSeconds(42))).isTrue();
        });
    assertThat(intents.findById(receipt.intentId()).orElseThrow().getState())
        .isEqualTo(NotificationIntentState.FANOUT_COMPLETE);
    assertThat(
            intents.findDueIds(
                NotificationIntentState.READY, NOW.plusSeconds(1000), PageRequest.of(0, 20)))
        .doesNotContain(receipt.intentId());
  }

  @Test
  void overlappingEnqueuesReturnTheSameCommittedIntention() throws Exception {
    long source = transaction.execute(status -> source());
    var ready = new CountDownLatch(2);
    var go = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS))
                  throw new IllegalStateException("start timed out");
                return transaction.execute(
                    status ->
                        queue.enqueue(
                            new NotificationIntentRequest(
                                source, NotificationKind.REINFORCED_REMINDER, NOW)));
              });
      var second =
          executor.submit(
              () -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS))
                  throw new IllegalStateException("start timed out");
                return transaction.execute(
                    status ->
                        queue.enqueue(
                            new NotificationIntentRequest(
                                source, NotificationKind.REINFORCED_REMINDER, NOW)));
              });
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      go.countDown();
      var one = first.get(20, TimeUnit.SECONDS);
      var two = second.get(20, TimeUnit.SECONDS);
      intentIds.add(one.intentId());
      intentIds.add(two.intentId());
      assertThat(one).isEqualTo(two);
    } finally {
      go.countDown();
    }
  }

  @Test
  void inconsistentStoredLeaseIsRejectedWithoutOverwritingOwnershipEvidence() {
    var receipt = transaction.execute(status -> enqueue(source()));
    jdbc.update(
        "update omission_notification_intents set lease_token = 'orphan' where id = ?",
        receipt.intentId());
    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    status ->
                        intents
                            .findForUpdate(receipt.intentId())
                            .orElseThrow()
                            .claimFanout("replacement", NOW, Duration.ofSeconds(30))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("stored notification intention is inconsistent");
    assertThat(
            jdbc.queryForObject(
                "select lease_token from omission_notification_intents where id = ?",
                String.class,
                receipt.intentId()))
        .isEqualTo("orphan");
    assertThat(intents.findById(receipt.intentId()).orElseThrow().getLeaseUntil()).isNull();
  }

  @Test
  void workerCannotCompleteBeforeItsReservationStartedOrAfterCancellation() {
    var receipt = transaction.execute(status -> enqueue(source()));
    transaction.executeWithoutResult(
        status -> {
          var intent = intents.findForUpdate(receipt.intentId()).orElseThrow();
          assertThat(intent.claimFanout("worker", NOW.plusSeconds(10), Duration.ofSeconds(30)))
              .isTrue();
          assertThat(intent.completeFanout("worker", NOW.plusSeconds(9))).isFalse();
          assertThat(intent.advanceFanout("worker", NOW.plusSeconds(9), "bad", "bad", "bad"))
              .isFalse();
          assertThat(intent.rescheduleFanout("worker", NOW.plusSeconds(9), Duration.ZERO))
              .isFalse();
        });
    var saved = intents.findById(receipt.intentId()).orElseThrow();
    assertThat(saved.getState()).isEqualTo(NotificationIntentState.READY);
    assertThat(saved.getCaregiverAfterId()).isEmpty();
    assertThat(saved.getLeaseUntil()).isEqualTo(NOW.plusSeconds(40));
    transaction.executeWithoutResult(
        status -> {
          var intent = intents.findForUpdate(receipt.intentId()).orElseThrow();
          intent.cancel();
          assertThat(intent.completeFanout("worker", NOW.plusSeconds(11))).isFalse();
          assertThat(
                  intent.claimFanout("replacement", NOW.plusSeconds(100), Duration.ofSeconds(30)))
              .isFalse();
        });
    var cancelled = intents.findById(receipt.intentId()).orElseThrow();
    assertThat(cancelled.getState()).isEqualTo(NotificationIntentState.CANCELLED);
    assertThat(cancelled.getLeaseUntil()).isNull();
    assertThat(
            intents.findDueIds(
                NotificationIntentState.READY, NOW.plusSeconds(1000), PageRequest.of(0, 20)))
        .doesNotContain(receipt.intentId());
  }
}
