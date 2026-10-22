package com.vitahealth.tata.omissionescalation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationIntentStore;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentRequest;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationCompletionProcessor;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationIntentState;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.NotificationDeviceDeliveryPersistenceEntity;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationDeviceDeliveryJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies that persisted aggregate sent evidence requires completed fanout and real acceptance.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationCompletionIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");
  @Autowired NotificationCompletionProcessor completion;
  @Autowired NotificationIntentStore queue;
  @Autowired NotificationIntentJpaRepository intents;
  @Autowired NotificationDeviceDeliveryJpaRepository deliveries;
  @Autowired IOmissionCaseRepository cases;
  @Autowired OmissionCaseJpaRepository caseRows;
  @Autowired PlatformTransactionManager manager;
  private long sourceId;
  private String intentId;

  private TransactionTemplate tx() {
    return new TransactionTemplate(manager);
  }

  private void fixture(NotificationKind kind) {
    tx().executeWithoutResult(
            status -> {
              var source =
                  new OmissionCase(
                      UUID.randomUUID().toString(),
                      UUID.randomUUID().toString(),
                      "Medication",
                      NOW,
                      GracePeriod.startingAt(NOW, Duration.ZERO));
              if (kind == NotificationKind.CAREGIVER_ALERT) {
                source.markOmitted(NOW);
                source.addAlert(NOW);
              }
              source = cases.save(source);
              sourceId = source.getId();
              intentId =
                  queue.enqueue(new NotificationIntentRequest(sourceId, kind, NOW)).intentId();
            });
  }

  private String device() {
    return tx().execute(
            status ->
                deliveries
                    .saveAndFlush(
                        new NotificationDeviceDeliveryPersistenceEntity(
                            intents.findById(intentId).orElseThrow(),
                            UUID.randomUUID().toString(),
                            "session",
                            "recipient",
                            NOW))
                    .getId());
  }

  private void accepted(String id, int seconds) {
    tx().executeWithoutResult(
            status -> {
              var delivery = deliveries.findForUpdate(id).orElseThrow();
              delivery.claim("worker", NOW, Duration.ofMinutes(1));
              assertThat(delivery.accept("worker", NOW.plusSeconds(seconds), "receipt-" + id))
                  .isTrue();
            });
  }

  private void finishFanout() {
    tx().executeWithoutResult(
            status -> {
              var intent = intents.findForUpdate(intentId).orElseThrow();
              intent.claimFanout("fanout", NOW, Duration.ofMinutes(1));
              intent.completeFanout("fanout", NOW.plusSeconds(1));
            });
  }

  @AfterEach
  void cleanup() {
    if (intentId != null)
      tx().executeWithoutResult(
              status -> {
                deliveries.deleteAllByIntent_Id(intentId);
                deliveries.flush();
                intents.deleteById(intentId);
                intents.flush();
                caseRows.deleteById(sourceId);
              });
  }

  @ParameterizedTest
  @EnumSource(NotificationKind.class)
  void onlyCompleteFanoutWithNoPendingDevicesMarksSourceOnce(NotificationKind kind) {
    fixture(kind);
    var first = device();
    var second = device();
    var originalAlert =
        cases.findById(sourceId).orElseThrow().getAlerts().stream().findFirst().orElse(null);
    accepted(first, 2);
    assertThat(completion.complete(intentId)).isFalse();
    finishFanout();
    assertThat(completion.complete(intentId)).isFalse();
    accepted(second, 3);
    assertThat(completion.complete(intentId)).isTrue();
    assertThat(completion.complete(intentId)).isFalse();
    var source = cases.findById(sourceId).orElseThrow();
    if (kind == NotificationKind.REINFORCED_REMINDER)
      assertThat(source.getReinforcedReminderSentAt()).isEqualTo(NOW.plusSeconds(3));
    else {
      var alert = source.getAlerts().getFirst();
      assertThat(alert.getStatus()).isEqualTo(AlertStatus.SENT);
      assertThat(alert.getSentAt()).isEqualTo(NOW.plusSeconds(3));
      assertThat(alert.getId()).isEqualTo(originalAlert.getId());
      assertThat(alert.getGeneratedAt()).isEqualTo(originalAlert.getGeneratedAt());
    }
    assertThat(intents.findById(intentId).orElseThrow().getState())
        .isEqualTo(NotificationIntentState.COMPLETED);
  }

  @Test
  void cancelledOnlyDeliveriesNeverCountAsSuccessfulProviderEvidence() {
    fixture(NotificationKind.REINFORCED_REMINDER);
    var id = device();
    tx().executeWithoutResult(status -> deliveries.findForUpdate(id).orElseThrow().cancel());
    finishFanout();
    assertThat(completion.complete(intentId)).isFalse();
    assertThat(cases.findById(sourceId).orElseThrow().getReinforcedReminderSentAt()).isNull();
  }
}
