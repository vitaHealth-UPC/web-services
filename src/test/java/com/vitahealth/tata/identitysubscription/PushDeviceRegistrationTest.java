package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Checks ownership, exact opaque-token rotation and timing without a provider dependency. */
class PushDeviceRegistrationTest {
  private static final Instant NOW = Instant.parse("2026-10-17T15:00:00Z");

  @Test
  void rotationPreservesOwnershipCreationAndOriginalSnapshot() {
    var initial =
        PushDeviceRegistration.register(UUID.randomUUID().toString(), "session", "old:token", NOW);
    var rotated = initial.rotate("new:token", NOW.plusSeconds(1));
    assertThat(rotated.installationId()).isEqualTo(initial.installationId());
    assertThat(rotated.sessionId()).isEqualTo("session");
    assertThat(rotated.createdAt()).isEqualTo(NOW);
    assertThat(rotated.updatedAt()).isEqualTo(NOW.plusSeconds(1));
    assertThat(rotated.token()).isEqualTo("new:token");
    assertThat(initial.token()).isEqualTo("old:token");
    assertThat(rotated.toString()).doesNotContain("new:token", "session");
  }

  @Test
  void invalidAddressAndBackwardRotationCannotChangeEvidence() {
    var initial =
        PushDeviceRegistration.register(UUID.randomUUID().toString(), "session", "old:token", NOW);
    for (String token :
        new String[] {null, "", " ", "bad token", "bad\u0000token", "x".repeat(4097)})
      assertThatThrownBy(() -> initial.rotate(token, NOW))
          .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> initial.rotate("new", NOW.minusNanos(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(initial.updatedAt()).isEqualTo(NOW);
    assertThat(initial.token()).isEqualTo("old:token");
  }
}
