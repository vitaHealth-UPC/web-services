package com.vitahealth.tata.omissionescalation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationIntentStore;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentRequest;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationDeliveryState;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.NotificationDeviceDeliveryPersistenceEntity;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationDeviceDeliveryJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies committed device outcomes and uniqueness against actual persistence transactions. */
@SpringBootTest
@ActiveProfiles("test")
class NotificationDeviceDeliveryPersistenceTest {
  private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");
  @Autowired IOmissionCaseRepository cases;
  @Autowired NotificationIntentStore queue;
  @Autowired NotificationIntentJpaRepository intents;
  @Autowired NotificationDeviceDeliveryJpaRepository deliveries;
  @Autowired PlatformTransactionManager manager;
  @Autowired OmissionCaseJpaRepository caseRows;

  @Test
  void recoveredReservationRetainsAcceptanceAndIndependentFailedDeviceAfterReload() {
    var tx = new TransactionTemplate(manager);
    var source =
        cases.save(
            new OmissionCase(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "Medication",
                NOW,
                GracePeriod.startingAt(NOW, Duration.ZERO)));
    String intentId =
        tx.execute(
            status ->
                queue
                    .enqueue(
                        new NotificationIntentRequest(
                            source.getId(), NotificationKind.CAREGIVER_ALERT, NOW))
                    .intentId());
    String first = null;
    String second = null;
    try {
      first =
          tx.execute(
              status ->
                  deliveries
                      .saveAndFlush(
                          new NotificationDeviceDeliveryPersistenceEntity(
                              intents.findById(intentId).orElseThrow(),
                              UUID.randomUUID().toString(),
                              "session-a",
                              "recipient-a",
                              NOW))
                      .getId());
      second =
          tx.execute(
              status ->
                  deliveries
                      .saveAndFlush(
                          new NotificationDeviceDeliveryPersistenceEntity(
                              intents.findById(intentId).orElseThrow(),
                              UUID.randomUUID().toString(),
                              "session-b",
                              "recipient-b",
                              NOW))
                      .getId());
      final String acceptedId = first;
      final String failedId = second;
      tx.executeWithoutResult(
          status -> {
            assertThat(
                    deliveries
                        .findForUpdate(acceptedId)
                        .orElseThrow()
                        .claim("old", NOW, Duration.ofSeconds(30)))
                .isTrue();
            var failed = deliveries.findForUpdate(failedId).orElseThrow();
            assertThat(failed.claim("failed", NOW, Duration.ofSeconds(30))).isTrue();
            assertThat(failed.retry("failed", NOW.plusSeconds(1))).isTrue();
          });
      tx.executeWithoutResult(
          status -> {
            var accepted = deliveries.findForUpdate(acceptedId).orElseThrow();
            assertThat(accepted.claim("replacement", NOW.plusSeconds(30), Duration.ofSeconds(30)))
                .isTrue();
            assertThat(accepted.accept("old", NOW.plusSeconds(31), "stale")).isFalse();
            assertThat(
                    accepted.accept(
                        "replacement", NOW.plusSeconds(31), "projects/tata/messages/receipt"))
                .isTrue();
          });
      var accepted = deliveries.findById(first).orElseThrow();
      var failed = deliveries.findById(second).orElseThrow();
      assertThat(accepted.getState()).isEqualTo(NotificationDeliveryState.ACCEPTED);
      assertThat(accepted.getProviderReceipt()).isEqualTo("projects/tata/messages/receipt");
      assertThat(accepted.getAcceptedAt()).isEqualTo(NOW.plusSeconds(31));
      assertThat(failed.getSessionId()).isEqualTo("session-b");
      assertThat(failed.getAttempts()).isEqualTo(1);
      assertThat(failed.getState()).isEqualTo(NotificationDeliveryState.READY);
      assertThat(
              deliveries.findDueIds(
                  NotificationDeliveryState.READY, NOW.plusSeconds(100), PageRequest.of(0, 100)))
          .contains(second)
          .doesNotContain(first);
      assertThatThrownBy(
              () ->
                  tx.executeWithoutResult(
                      status ->
                          deliveries.saveAndFlush(
                              new NotificationDeviceDeliveryPersistenceEntity(
                                  intents.findById(intentId).orElseThrow(),
                                  accepted.getInstallationId(),
                                  "different-session",
                                  "different-recipient",
                                  NOW))))
          .isInstanceOf(DataIntegrityViolationException.class);
      assertThat(deliveries.findById(first).orElseThrow().getProviderReceipt())
          .isEqualTo(accepted.getProviderReceipt());
    } finally {
      if (first != null) deliveries.deleteById(first);
      if (second != null) deliveries.deleteById(second);
      intents.deleteById(intentId);
      caseRows.deleteById(source.getId());
    }
  }
}
