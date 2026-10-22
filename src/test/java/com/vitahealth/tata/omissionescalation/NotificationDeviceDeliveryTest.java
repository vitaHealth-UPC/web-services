package com.vitahealth.tata.omissionescalation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationDeliveryState;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.NotificationDeviceDeliveryPersistenceEntity;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.NotificationIntentPersistenceEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Checks independent device attempt outcomes and stale-worker rejection before provider wiring. */
class NotificationDeviceDeliveryTest {
  private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");

  private NotificationDeviceDeliveryPersistenceEntity delivery() {
    var intent = mock(NotificationIntentPersistenceEntity.class);
    when(intent.getRequestedAt()).thenReturn(NOW);
    return new NotificationDeviceDeliveryPersistenceEntity(
        intent, UUID.randomUUID().toString(), "original-session", "recipient", NOW);
  }

  @Test
  void expiredWorkerCannotOverwriteAcceptanceByReplacementWorker() {
    var delivery = delivery();
    assertThat(delivery.claim("first", NOW, Duration.ofSeconds(30))).isTrue();
    assertThat(delivery.claim("second", NOW.plusSeconds(29), Duration.ofSeconds(30))).isFalse();
    assertThat(delivery.accept("first", NOW.minusSeconds(1), "invalid")).isFalse();
    assertThat(delivery.accept("first", NOW.plusSeconds(30), "expired")).isFalse();
    assertThat(delivery.claim("second", NOW.plusSeconds(30), Duration.ofSeconds(30))).isTrue();
    assertThat(delivery.accept("first", NOW.plusSeconds(31), "stale")).isFalse();
    assertThat(delivery.accept("second", NOW.plusSeconds(31), "provider-accepted")).isTrue();
    delivery.cancel();
    assertThat(delivery.getState()).isEqualTo(NotificationDeliveryState.ACCEPTED);
    assertThat(delivery.getProviderReceipt()).isEqualTo("provider-accepted");
    assertThat(delivery.getAcceptedAt()).isEqualTo(NOW.plusSeconds(31));
    assertThat(delivery.retry("second", NOW.plusSeconds(32))).isFalse();
  }

  @Test
  void failureOnlyReschedulesItsOwnDeviceAndRetainsOriginalOwnership() {
    var accepted = delivery();
    var failed = delivery();
    accepted.claim("one", NOW, Duration.ofSeconds(30));
    failed.claim("two", NOW, Duration.ofSeconds(30));
    accepted.accept("one", NOW.plusSeconds(1), "receipt");
    assertThat(failed.retry("two", NOW.plusSeconds(1))).isTrue();
    assertThat(failed.getAttempts()).isEqualTo(1);
    assertThat(failed.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(11));
    assertThat(failed.claim("three", NOW.plusSeconds(10), Duration.ofSeconds(30))).isFalse();
    assertThat(failed.claim("three", NOW.plusSeconds(11), Duration.ofSeconds(30))).isTrue();
    assertThat(failed.getSessionId()).isEqualTo("original-session");
    assertThat(failed.getProviderReceipt()).isNull();
    assertThat(accepted.getState()).isEqualTo(NotificationDeliveryState.ACCEPTED);
  }

  @Test
  void invalidReceiptDoesNotConsumeWorkerReservationAndCancellationCannotBeReclaimed() {
    var delivery = delivery();
    delivery.claim("worker", NOW, Duration.ofSeconds(30));
    assertThatThrownBy(() -> delivery.accept("worker", NOW.plusSeconds(1), " "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(delivery.owns("worker", NOW.plusSeconds(1))).isTrue();
    assertThat(delivery.getAcceptedAt()).isNull();
    delivery.cancel();
    assertThat(delivery.claim("replacement", NOW.plusSeconds(60), Duration.ofSeconds(30)))
        .isFalse();
    assertThat(delivery.getState()).isEqualTo(NotificationDeliveryState.CANCELLED);
  }
}
