package com.tata.omissionescalation.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.tata.omissionescalation.infrastructure.external.adapters.PushNotificationAdapter;
import com.tata.omissionescalation.infrastructure.external.dto.PushProviderMessage;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PushNotificationAdapterTest {

  @Test
  void reinforcedReminder_mapsApplicationDataToProviderMessage() {
    AtomicReference<PushProviderMessage> captured = new AtomicReference<>();
    PushNotificationAdapter adapter = new PushNotificationAdapter(captured::set);

    var result = adapter.sendReinforcedReminder(
        "adult-10",
        "Losartan 50 mg",
        Instant.parse("2026-10-06T13:00:00Z"));

    assertThat(result.delivered()).isTrue();
    assertThat(captured.get().recipient()).isEqualTo("user-adult-10");
    assertThat(captured.get().title()).isEqualTo("Medication reminder");
    assertThat(captured.get().body()).contains("Losartan 50 mg");
  }

  @Test
  void caregiverAlert_mapsToCaregiverAudience() {
    AtomicReference<PushProviderMessage> captured = new AtomicReference<>();
    PushNotificationAdapter adapter = new PushNotificationAdapter(captured::set);

    var result = adapter.sendCaregiverAlert(
        "adult-10",
        "Losartan 50 mg",
        Instant.parse("2026-10-06T13:00:00Z"));

    assertThat(result.delivered()).isTrue();
    assertThat(captured.get().recipient()).isEqualTo("caregivers-of-adult-10");
    assertThat(captured.get().title()).isEqualTo("Medication not confirmed");
  }

  @Test
  void providerFailure_isReturnedWithoutThrowing() {
    PushNotificationAdapter adapter = new PushNotificationAdapter(message -> {
      throw new IllegalStateException("provider unavailable");
    });

    var result = adapter.sendCaregiverAlert(
        "adult-10",
        "Losartan 50 mg",
        Instant.parse("2026-10-06T13:00:00Z"));

    assertThat(result.delivered()).isFalse();
    assertThat(result.failureReason()).isEqualTo("provider unavailable");
  }
}
