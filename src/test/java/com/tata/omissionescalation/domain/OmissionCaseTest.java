package com.tata.omissionescalation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tata.omissionescalation.domain.factories.OmissionCaseFactory;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.services.EscalationPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OmissionCaseTest {

  private static final Instant START = Instant.parse("2026-10-05T13:00:00Z");
  private static final Instant AFTER_GRACE = START.plus(Duration.ofMinutes(31));

  private OmissionCase pendingCase() {
    return OmissionCaseFactory.createPending(
        "intake-1", "adult-10", "Losartan 50 mg", START, GracePeriod.startingAt(START, Duration.ofMinutes(30)));
  }

  private OmissionCase omittedCase() {
    OmissionCase omissionCase = pendingCase();
    omissionCase.markOmitted(AFTER_GRACE);
    return omissionCase;
  }

  @Test
  void resolve_whenConfirmedDuringGracePeriod_setsStatusResolved() {
    OmissionCase omissionCase = pendingCase();

    omissionCase.resolve();

    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.RESOLVED);
  }

  @Test
  void resolve_cannotReplaceADefinitiveOmission() {
    OmissionCase omissionCase = omittedCase();
    assertThatThrownBy(omissionCase::resolve).isInstanceOf(IllegalStateException.class);
    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.OMITTED);
    assertThat(omissionCase.getOmittedAt()).isEqualTo(AFTER_GRACE);
  }

  @Test
  void markOmitted_beforeGracePeriodEnds_isRejected() {
    OmissionCase omissionCase = pendingCase();

    assertThatThrownBy(() -> omissionCase.markOmitted(START.plus(Duration.ofMinutes(5))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void markOmitted_afterGracePeriod_setsStatusOmitted() {
    OmissionCase omissionCase = omittedCase();

    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.OMITTED);
    assertThat(omissionCase.getOmittedAt()).isEqualTo(AFTER_GRACE);
  }

  @Test
  void addAlert_calledTwice_keepsOneAlert() {
    OmissionCase omissionCase = omittedCase();

    omissionCase.addAlert(AFTER_GRACE);
    omissionCase.addAlert(AFTER_GRACE.plusSeconds(60));

    assertThat(omissionCase.getAlerts()).hasSize(1);
  }

  @Test
  void escalate_raisesLevelEachTime() {
    OmissionCase omissionCase = omittedCase();

    omissionCase.escalate("still unresolved", AFTER_GRACE.plus(Duration.ofMinutes(30)));
    omissionCase.escalate("still unresolved", AFTER_GRACE.plus(Duration.ofMinutes(60)));

    assertThat(omissionCase.currentLevel().value()).isEqualTo(2);
    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.ESCALATED);
    assertThat(omissionCase.getEscalations()).hasSize(2);
  }

  @Test
  void escalationPolicy_waitsForTheInterval() {
    OmissionCase omissionCase = omittedCase();
    EscalationPolicy policy = new EscalationPolicy();

    assertThat(policy.shouldEscalate(omissionCase, AFTER_GRACE.plus(Duration.ofMinutes(29))))
        .isFalse();
    assertThat(policy.shouldEscalate(omissionCase, AFTER_GRACE.plus(Duration.ofMinutes(30))))
        .isTrue();
  }

  @Test
  void escalationPolicy_stopsAtTheMaximumLevel() {
    OmissionCase omissionCase = omittedCase();
    Instant at = AFTER_GRACE;
    for (int level = 1; level <= 3; level++) {
      at = at.plus(Duration.ofMinutes(30));
      omissionCase.escalate("still unresolved", at);
    }

    assertThat(new EscalationPolicy().shouldEscalate(omissionCase, at.plus(Duration.ofDays(1))))
        .isFalse();
  }

  @Test
  void escalationPolicy_ignoresResolvedCases() {
    OmissionCase omissionCase = pendingCase();
    omissionCase.resolve();

    assertThat(new EscalationPolicy().shouldEscalate(omissionCase, AFTER_GRACE.plusSeconds(3600)))
        .isFalse();
  }

  @Test
  void close_keepsAlertsAndEscalations() {
    OmissionCase omissionCase = omittedCase();
    omissionCase.addAlert(AFTER_GRACE);
    omissionCase.escalate("still unresolved", AFTER_GRACE.plus(Duration.ofMinutes(30)));

    omissionCase.close(AFTER_GRACE.plus(Duration.ofMinutes(31)));

    assertThat(omissionCase.getStatus()).isEqualTo(OmissionCaseStatus.CLOSED);
    assertThat(omissionCase.getAlerts()).hasSize(1);
    assertThat(omissionCase.getEscalations()).hasSize(1);
  }
}
