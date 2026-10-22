package com.vitahealth.tata.omissionescalation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationPreferences;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Verifies exact quiet-hour boundaries and explicit zone interpretation across midnight. */
class NotificationPreferencesTest {
  @Test
  void sameInstantRespectsConfiguredZoneAndOvernightExclusiveEnd() {
    var start = LocalTime.of(22, 0);
    var end = LocalTime.of(7, 0);
    var lima = new NotificationPreferences(true, start, end, ZoneId.of("America/Lima"));
    var utc = new NotificationPreferences(true, start, end, ZoneId.of("UTC"));
    assertThat(lima.isQuietAt(Instant.parse("2026-10-18T03:00:00Z"))).isTrue();
    assertThat(lima.isQuietAt(Instant.parse("2026-10-18T12:00:00Z"))).isFalse();
    assertThat(lima.isQuietAt(Instant.parse("2026-10-18T02:59:59Z"))).isFalse();
    assertThat(utc.isQuietAt(Instant.parse("2026-10-18T10:00:00Z"))).isFalse();
    assertThat(lima.isQuietAt(Instant.parse("2026-10-18T10:00:00Z"))).isTrue();
  }

  @Test
  void partialEqualAndSubminuteBoundariesAreRejected() {
    var time = LocalTime.of(7, 0);
    var zone = ZoneId.of("UTC");
    assertThatThrownBy(() -> new NotificationPreferences(true, time, null, zone))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new NotificationPreferences(true, time, time, zone))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new NotificationPreferences(true, time.withSecond(1), LocalTime.of(8, 0), zone))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new NotificationPreferences(true, null, null, zone).isQuietAt(Instant.EPOCH))
        .isFalse();
  }
}
