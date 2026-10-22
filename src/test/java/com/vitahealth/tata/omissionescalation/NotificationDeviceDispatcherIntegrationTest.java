package com.vitahealth.tata.omissionescalation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationDeviceTransport;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationIntentStore;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationRecipientPort;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDeviceAddress;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDevicePage;
import com.vitahealth.tata.omissionescalation.application.result.NotificationDispatchRequest;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentRequest;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationPreferences;
import com.vitahealth.tata.omissionescalation.domain.ports.INotificationPreferencesPort;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationDeliveryScheduler;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationDeliveryState;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationDeviceDispatcher;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationIntentState;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.NotificationDeviceDeliveryPersistenceEntity;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationDeviceDeliveryJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.NotificationIntentJpaRepository;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies real reservation commit and provider execution outside all database transactions. */
@SpringBootTest
@ActiveProfiles("test")
class NotificationDeviceDispatcherIntegrationTest {
  @Autowired NotificationDeviceDispatcher dispatcher;
  @Autowired NotificationDeliveryScheduler scheduler;
  @Autowired NotificationIntentStore queue;
  @Autowired NotificationIntentJpaRepository intents;
  @Autowired NotificationDeviceDeliveryJpaRepository deliveries;
  @Autowired IOmissionCaseRepository cases;
  @Autowired OmissionCaseJpaRepository caseRows;
  @Autowired PlatformTransactionManager manager;
  @MockitoSpyBean Clock clock;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean NotificationRecipientPort recipients;
  @MockitoBean NotificationDeviceTransport transport;
  @MockitoBean INotificationPreferencesPort preferences;
  private String intentId;
  private String deliveryId;
  private long sourceId;

  private void fixture() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              var now = clock.instant();
              var source =
                  cases.save(
                      new OmissionCase(
                          UUID.randomUUID().toString(),
                          UUID.randomUUID().toString(),
                          "Medication",
                          now,
                          GracePeriod.startingAt(now, Duration.ofHours(1))));
              sourceId = source.getId();
              intentId =
                  queue
                      .enqueue(
                          new NotificationIntentRequest(
                              sourceId, NotificationKind.REINFORCED_REMINDER, now))
                      .intentId();
              deliveryId =
                  deliveries
                      .saveAndFlush(
                          new NotificationDeviceDeliveryPersistenceEntity(
                              intents.findById(intentId).orElseThrow(),
                              UUID.randomUUID().toString(),
                              "original-session",
                              source.getOlderAdultId(),
                              now))
                      .getId();
            });
    var delivery = deliveries.findById(deliveryId).orElseThrow();
    when(recipients.findOlderAdultDevice(anyString(), anyString(), anyString()))
        .thenReturn(
            Optional.of(
                new NotificationDeviceAddress(
                    delivery.getInstallationId(), delivery.getSessionId(), "latest-token")));
    when(preferences.getPreferences(anyString()))
        .thenReturn(new NotificationPreferences(true, null, null, ZoneId.of("America/Lima")));
  }

  @AfterEach
  void cleanup() {
    if (deliveryId != null) deliveries.deleteById(deliveryId);
    if (intentId != null) intents.deleteById(intentId);
    if (sourceId != 0) caseRows.deleteById(sourceId);
  }

  @Test
  void providerRunsAfterLeaseCommitOutsideAmbientTransactionAndAcceptanceIsNotResent() {
    fixture();
    when(transport.send(any()))
        .thenAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              var request = invocation.getArgument(0, NotificationDispatchRequest.class);
              assertThat(request.deviceToken()).isEqualTo("latest-token");
              assertThat(request.deliveryId()).isEqualTo(deliveryId);
              assertThat(
                      jdbc.queryForObject(
                          "select lease_token from omission_device_deliveries where id = ?",
                          String.class,
                          deliveryId))
                  .isNotBlank();
              new TransactionTemplate(manager)
                  .executeWithoutResult(
                      status ->
                          assertThat(
                                  deliveries
                                      .findById(deliveryId)
                                      .orElseThrow()
                                      .owns("not-the-worker", clock.instant()))
                              .isFalse());
              return "projects/tata/messages/accepted";
            });
    new TransactionTemplate(manager)
        .executeWithoutResult(status -> dispatcher.dispatch(deliveryId));
    dispatcher.dispatch(deliveryId);
    var accepted = deliveries.findById(deliveryId).orElseThrow();
    assertThat(accepted.getState()).isEqualTo(NotificationDeliveryState.ACCEPTED);
    assertThat(accepted.getProviderReceipt()).isEqualTo("projects/tata/messages/accepted");
    verify(transport, times(1)).send(any());
    assertThat(cases.findById(sourceId).orElseThrow().getReinforcedReminderSentAt()).isNull();
  }

  @Test
  void revokedAddressCancelsWithoutProviderCall() {
    fixture();
    when(recipients.findOlderAdultDevice(anyString(), anyString(), anyString()))
        .thenReturn(Optional.empty());
    dispatcher.dispatch(deliveryId);
    assertThat(deliveries.findById(deliveryId).orElseThrow().getState())
        .isEqualTo(NotificationDeliveryState.CANCELLED);
    verifyNoInteractions(transport);
  }

  @Test
  void providerFailureSchedulesRetryWithoutPersistingSensitiveError() {
    fixture();
    when(transport.send(any()))
        .thenThrow(new IllegalStateException("secret private-medication latest-token"));
    dispatcher.dispatch(deliveryId);
    var pending = deliveries.findById(deliveryId).orElseThrow();
    assertThat(pending.getState()).isEqualTo(NotificationDeliveryState.READY);
    assertThat(pending.getAttempts()).isEqualTo(1);
    assertThat(pending.getProviderReceipt()).isNull();
    assertThat(pending.getNextAttemptAt()).isAfter(clock.instant());
  }

  @Test
  void expiredProviderCallbackCannotRecordAcceptanceAndRetryKeepsDeliveryIdentity() {
    fixture();
    var time = new AtomicReference<>(clock.instant());
    doAnswer(invocation -> time.get()).when(clock).instant();
    when(transport.send(any()))
        .thenAnswer(
            invocation -> {
              time.set(time.get().plusSeconds(61));
              return "late-receipt";
            });
    dispatcher.dispatch(deliveryId);
    assertThat(deliveries.findById(deliveryId).orElseThrow().getProviderReceipt()).isNull();
    assertThat(deliveries.findById(deliveryId).orElseThrow().getState())
        .isEqualTo(NotificationDeliveryState.READY);
    when(transport.send(any()))
        .thenAnswer(
            invocation -> {
              assertThat(invocation.getArgument(0, NotificationDispatchRequest.class).deliveryId())
                  .isEqualTo(deliveryId);
              return "replacement-receipt";
            });
    dispatcher.dispatch(deliveryId);
    assertThat(deliveries.findById(deliveryId).orElseThrow().getProviderReceipt())
        .isEqualTo("replacement-receipt");
    verify(transport, times(2)).send(any());
  }

  @Test
  void disabledPreferenceDefersWithoutSending() {
    fixture();
    when(preferences.getPreferences(anyString()))
        .thenReturn(new NotificationPreferences(false, null, null, ZoneId.of("America/Lima")));
    dispatcher.dispatch(deliveryId);
    assertThat(deliveries.findById(deliveryId).orElseThrow().getState())
        .isEqualTo(NotificationDeliveryState.READY);
    assertThat(deliveries.findById(deliveryId).orElseThrow().getAttempts()).isEqualTo(1);
    verifyNoInteractions(transport);
  }

  @Test
  void resolvedSourceCancelsWithoutResolvingDeviceOrCallingProvider() {
    fixture();
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              var source = cases.findByIdForUpdate(sourceId).orElseThrow();
              source.resolve();
              cases.save(source);
            });
    dispatcher.dispatch(deliveryId);
    assertThat(deliveries.findById(deliveryId).orElseThrow().getState())
        .isEqualTo(NotificationDeliveryState.CANCELLED);
    verifyNoInteractions(transport);
  }

  @Test
  void schedulerTickCommitsFanoutDeviceAcceptanceAndSourceCompletion() {
    fixture();
    var device = deliveries.findById(deliveryId).orElseThrow();
    when(recipients.findOlderAdultDevices(anyString(), anyString(), anyInt()))
        .thenReturn(
            new NotificationDevicePage(
                List.of(
                    new NotificationDeviceAddress(
                        device.getInstallationId(), device.getSessionId(), "latest-token")),
                null));
    when(transport.send(any())).thenReturn("projects/tata/messages/tick");
    scheduler.dispatch();
    assertThat(deliveries.findById(deliveryId).orElseThrow().getState())
        .isEqualTo(NotificationDeliveryState.ACCEPTED);
    assertThat(intents.findById(intentId).orElseThrow().getState())
        .isEqualTo(NotificationIntentState.COMPLETED);
    assertThat(cases.findById(sourceId).orElseThrow().getReinforcedReminderSentAt()).isNotNull();
    scheduler.dispatch();
    verify(transport, times(1)).send(any());
  }

  @Test
  void confirmationDuringDeviceLookupCancelsBeforeExternalSend() {
    fixture();
    var device = deliveries.findById(deliveryId).orElseThrow();
    when(recipients.findOlderAdultDevice(anyString(), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              new TransactionTemplate(manager)
                  .executeWithoutResult(
                      status -> {
                        var source = cases.findByIdForUpdate(sourceId).orElseThrow();
                        source.resolve();
                        cases.save(source);
                      });
              return Optional.of(
                  new NotificationDeviceAddress(
                      device.getInstallationId(), device.getSessionId(), "latest-token"));
            });
    dispatcher.dispatch(deliveryId);
    verifyNoInteractions(transport);
    assertThat(deliveries.findById(deliveryId).orElseThrow().getState())
        .isEqualTo(NotificationDeliveryState.CANCELLED);
  }

  @Test
  void authorizationStorageOutagePropagatesWithoutRecordingAProviderFailure() {
    fixture();
    var outage = new DataAccessResourceFailureException("permission store unavailable");
    when(recipients.findOlderAdultDevice(anyString(), anyString(), anyString())).thenThrow(outage);
    assertThatThrownBy(() -> dispatcher.dispatch(deliveryId)).isSameAs(outage);
    var pending = deliveries.findById(deliveryId).orElseThrow();
    assertThat(pending.getState()).isEqualTo(NotificationDeliveryState.READY);
    assertThat(pending.getAttempts()).isZero();
    assertThat(pending.getProviderReceipt()).isNull();
    assertThat(
            jdbc.queryForObject(
                "select lease_token from omission_device_deliveries where id = ?",
                String.class,
                deliveryId))
        .isNotBlank();
    verifyNoInteractions(transport);
  }
}
