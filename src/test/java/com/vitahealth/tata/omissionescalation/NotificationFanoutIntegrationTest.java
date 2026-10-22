package com.vitahealth.tata.omissionescalation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationIntentStore;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationRecipientPort;
import com.vitahealth.tata.omissionescalation.application.result.NotificationCaregiverPage;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDeviceAddress;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDevicePage;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentRequest;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationFanoutPageProcessor;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationIntentState;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationDeviceDeliveryJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exercises atomic device materialization and cursor persistence with real transaction rollback.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationFanoutIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");
  @Autowired NotificationIntentStore queue;
  @Autowired NotificationIntentJpaRepository intents;
  @Autowired NotificationDeviceDeliveryJpaRepository deliveries;
  @Autowired IOmissionCaseRepository cases;
  @Autowired OmissionCaseJpaRepository caseRows;
  @Autowired NotificationFanoutPageProcessor fanout;
  @Autowired PlatformTransactionManager manager;
  @MockitoBean NotificationRecipientPort recipients;
  @MockitoSpyBean Clock clock;
  private final List<String> ownedIntents = new ArrayList<>();
  private final List<Long> ownedCases = new ArrayList<>();

  @AfterEach
  void cleanup() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              for (String id : ownedIntents) {
                deliveries.deleteAllByIntent_Id(id);
                deliveries.flush();
                intents.deleteById(id);
              }
              intents.flush();
              caseRows.deleteAllById(ownedCases);
            });
  }

  private String intent() {
    return new TransactionTemplate(manager)
        .execute(
            status -> {
              var source =
                  cases.save(
                      new OmissionCase(
                          UUID.randomUUID().toString(),
                          UUID.randomUUID().toString(),
                          "Medication",
                          NOW,
                          GracePeriod.startingAt(NOW, Duration.ofHours(1))));
              ownedCases.add(source.getId());
              var id =
                  queue
                      .enqueue(
                          new NotificationIntentRequest(
                              source.getId(), NotificationKind.REINFORCED_REMINDER, NOW))
                      .intentId();
              ownedIntents.add(id);
              return id;
            });
  }

  private NotificationDeviceAddress device() {
    return new NotificationDeviceAddress(
        UUID.randomUUID().toString(), "session", "never-persist-this-token");
  }

  @Test
  void pagesCommitDeviceOwnershipAndCursorWithoutMarkingReminderSent() {
    var first = device();
    var last = device();
    var id = intent();
    when(recipients.findOlderAdultDevices(anyString(), anyString(), anyInt()))
        .thenReturn(
            new NotificationDevicePage(List.of(first), first.installationId()),
            new NotificationDevicePage(List.of(first, last), null));
    fanout.process(id);
    assertThat(intents.findById(id).orElseThrow().getDeviceAfterId())
        .isEqualTo(first.installationId());
    assertThat(deliveries.findByIntent_IdAndInstallationId(id, first.installationId())).isPresent();
    fanout.process(id);
    assertThat(intents.findById(id).orElseThrow().getState())
        .isEqualTo(NotificationIntentState.FANOUT_COMPLETE);
    assertThat(
            deliveries
                .findByIntent_IdAndInstallationId(id, last.installationId())
                .orElseThrow()
                .getSessionId())
        .isEqualTo("session");
    assertThat(cases.findById(ownedCases.getFirst()).orElseThrow().getReinforcedReminderSentAt())
        .isNull();
  }

  @Test
  void failedPageRollsBackEveryDeviceAndItsLeaseAndCursor() {
    var first = device();
    var id = intent();
    when(recipients.findOlderAdultDevices(anyString(), anyString(), anyInt()))
        .thenReturn(
            new NotificationDevicePage(
                List.of(first, new NotificationDeviceAddress("invalid-id", "session", "token")),
                "cursor"));
    assertThatThrownBy(() -> fanout.process(id)).isInstanceOf(IllegalArgumentException.class);
    assertThat(deliveries.existsByIntent_Id(id)).isFalse();
    var stored = intents.findById(id).orElseThrow();
    assertThat(stored.getDeviceAfterId()).isEmpty();
    assertThat(stored.getLeaseUntil()).isNull();
    when(recipients.findOlderAdultDevices(anyString(), anyString(), anyInt()))
        .thenReturn(new NotificationDevicePage(List.of(first), null));
    fanout.process(id);
    assertThat(deliveries.findByIntent_IdAndInstallationId(id, first.installationId())).isPresent();
  }

  @Test
  void noRecipientsRemainsRetryableAndNeverBecomesCompleted() {
    var id = intent();
    when(recipients.findOlderAdultDevices(anyString(), anyString(), anyInt()))
        .thenReturn(new NotificationDevicePage(List.of(), null));
    fanout.process(id);
    var stored = intents.findById(id).orElseThrow();
    assertThat(stored.getState()).isEqualTo(NotificationIntentState.READY);
    assertThat(stored.getLeaseUntil()).isNull();
    assertThat(deliveries.existsByIntent_Id(id)).isFalse();
  }

  @Test
  void caregiverDevicePagesResumeBeforeAdvancingToTheNextCaregiver() {
    var id =
        new TransactionTemplate(manager)
            .execute(
                status -> {
                  var source =
                      cases.save(
                          new OmissionCase(
                              UUID.randomUUID().toString(),
                              UUID.randomUUID().toString(),
                              "Medication",
                              NOW,
                              GracePeriod.startingAt(NOW, Duration.ZERO)));
                  source.markOmitted(NOW);
                  source = cases.save(source);
                  ownedCases.add(source.getId());
                  var receipt =
                      queue.enqueue(
                          new NotificationIntentRequest(
                              source.getId(), NotificationKind.CAREGIVER_ALERT, NOW));
                  ownedIntents.add(receipt.intentId());
                  return receipt.intentId();
                });
    var first = device();
    var second = device();
    var other = device();
    when(recipients.findCaregivers(anyString(), anyString(), anyInt()))
        .thenReturn(
            new NotificationCaregiverPage(List.of("caregiver-a"), "caregiver-a"),
            new NotificationCaregiverPage(List.of("caregiver-b"), "caregiver-b"),
            new NotificationCaregiverPage(List.of(), null));
    when(recipients.findCaregiverDevices(anyString(), anyString(), anyString(), anyInt()))
        .thenReturn(
            new NotificationDevicePage(List.of(first), first.installationId()),
            new NotificationDevicePage(List.of(second), null),
            new NotificationDevicePage(List.of(other), null));
    fanout.process(id);
    var pending = intents.findById(id).orElseThrow();
    assertThat(pending.getCurrentCaregiverId()).isEqualTo("caregiver-a");
    assertThat(pending.getCaregiverAfterId()).isEmpty();
    assertThat(pending.getDeviceAfterId()).isEqualTo(first.installationId());
    fanout.process(id);
    var resumed = intents.findById(id).orElseThrow();
    assertThat(resumed.getCurrentCaregiverId()).isNull();
    assertThat(resumed.getCaregiverAfterId()).isEqualTo("caregiver-a");
    fanout.process(id);
    fanout.process(id);
    assertThat(intents.findById(id).orElseThrow().getState())
        .isEqualTo(NotificationIntentState.FANOUT_COMPLETE);
    assertThat(
            deliveries
                .findByIntent_IdAndInstallationId(id, first.installationId())
                .orElseThrow()
                .getRecipientId())
        .isEqualTo("caregiver-a");
    assertThat(
            deliveries
                .findByIntent_IdAndInstallationId(id, second.installationId())
                .orElseThrow()
                .getRecipientId())
        .isEqualTo("caregiver-a");
    assertThat(
            deliveries
                .findByIntent_IdAndInstallationId(id, other.installationId())
                .orElseThrow()
                .getRecipientId())
        .isEqualTo("caregiver-b");
  }

  @Test
  void leaseExpiringDuringRecipientLookupRollsBackThePage() {
    var id = intent();
    var first = device();
    var time = new AtomicReference<>(clock.instant());
    doAnswer(invocation -> time.get()).when(clock).instant();
    when(recipients.findOlderAdultDevices(anyString(), anyString(), anyInt()))
        .thenAnswer(
            invocation -> {
              time.set(time.get().plusSeconds(61));
              return new NotificationDevicePage(List.of(first), null);
            });
    assertThatThrownBy(() -> fanout.process(id))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("notification fanout reservation was lost");
    assertThat(deliveries.existsByIntent_Id(id)).isFalse();
    assertThat(intents.findById(id).orElseThrow().getLeaseUntil()).isNull();
    assertThat(intents.findById(id).orElseThrow().getDeviceAfterId()).isEmpty();
  }
}
