package com.vitahealth.tata.carelink.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Verifies exclusive expiration and complete immutable consent/linking evidence. */
class LinkingEvidenceTest {
  private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

  @Test
  void linkingUseIsImmutableAndExpirationIsExclusive() {
    var code = LinkingCode.issue("\u2003tata-4821\u2003", NOW, Duration.ofMinutes(1));
    assertThat(code.value()).isEqualTo("TATA-4821");
    assertThat(code.isValid(NOW.plusSeconds(60))).isFalse();
    assertThatIllegalStateException().isThrownBy(() -> code.useAt(NOW.plusSeconds(60)));
    var used = code.useAt(NOW.plusSeconds(59));
    assertThat(used.wasUsedWithinValidity()).isTrue();
    assertThat(code.usedAt()).isNull();
    assertThatIllegalStateException().isThrownBy(() -> used.useAt(NOW.plusSeconds(59)));
  }

  @Test
  void storedUsageAtExpirationCannotBecomeValidConfirmationEvidence() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LinkingCode.rehydrate("TATA-4821", NOW, NOW));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LinkingCode.rehydrate("TATA-4821", NOW, NOW.plusSeconds(1)));
  }

  @Test
  void consentApprovalRequiresRecordedEvidenceButPendingAndRejectionRemainDistinct() {
    assertThatIllegalArgumentException().isThrownBy(() -> Consent.rehydrate(true, null));
    assertThat(Consent.pending().recordedAt()).isNull();
    assertThat(Consent.record(false, NOW).recordedAt()).isEqualTo(NOW);
    assertThat(Consent.record(false, NOW).isGranted()).isFalse();
    assertThat(Consent.record(true, NOW).isGranted()).isTrue();
  }
}
