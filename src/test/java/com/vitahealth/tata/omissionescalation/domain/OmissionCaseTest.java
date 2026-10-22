package com.vitahealth.tata.omissionescalation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.services.EscalationPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OmissionCaseTest {

  private static final Instant START = Instant.parse("2026-10-05T13:00:00Z");
  private static final Instant AFTER_GRACE = START.plus(Duration.ofMinutes(31));

  private OmissionCase pendingCase() {
    return new OmissionCase(
        "intake-1",
        "adult-10",
        "Losartan 50 mg",
        START,
        GracePeriod.startingAt(START, Duration.ofMinutes(30)));
  }

  private OmissionCase omittedCase() {
    OmissionCase omissionCase = pendingCase();
    omissionCase.markOmitted(AFTER_GRACE);
    return omissionCase;
  }

  @Test
  void returnedAlertCannotChangeTheAggregateDeliveryEvidence() {
    var source = omittedCase();
    var generated = source.addAlert(AFTER_GRACE);
    var independent = generated.withSuccessfulDelivery(AFTER_GRACE.plusSeconds(60));

    assertThat(independent.getStatus()).isEqualTo(AlertStatus.SENT);
    assertThat(source.getAlerts().getFirst().getStatus()).isEqualTo(AlertStatus.GENERATED);
    assertThat(source.getAlerts().getFirst().getSentAt()).isNull();

    source.markAlertSent(AFTER_GRACE.plusSeconds(60));
    assertThat(generated.getStatus()).isEqualTo(AlertStatus.GENERATED);
    assertThat(source.getAlerts().getFirst().getStatus()).isEqualTo(AlertStatus.SENT);
  }

  @Test
  void alertAcceptanceRetainsFirstSuccessAndRejectsLaterFailure() {
    var source = omittedCase();
    source.addAlert(AFTER_GRACE);
    source.markAlertFailed("historical provider failure");
    var failed = source.getAlerts().getFirst();
    source.markAlertSent(AFTER_GRACE.plusSeconds(60));
    source.markAlertSent(AFTER_GRACE.plusSeconds(120));

    assertThat(source.getAlerts().getFirst().getSentAt()).isEqualTo(AFTER_GRACE.plusSeconds(60));
    assertThat(source.getAlerts().getFirst().getGeneratedAt()).isEqualTo(AFTER_GRACE);
    assertThat(source.getAlerts().getFirst().getFailureReason()).isNull();
    assertThat(failed.getStatus()).isEqualTo(AlertStatus.FAILED);
    assertThatThrownBy(() -> source.markAlertFailed("late failure"))
        .isInstanceOf(IllegalStateException.class);
    assertThat(source.getAlerts().getFirst().getSentAt()).isEqualTo(AFTER_GRACE.plusSeconds(60));
  }

  @Test
  void invalidAlertDeliveryLeavesTheOwnedSnapshotUnchanged() {
    var source = omittedCase();
    var original = source.addAlert(AFTER_GRACE);

    assertThatThrownBy(() -> source.markAlertSent(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> source.markAlertSent(AFTER_GRACE.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> source.markAlertFailed(" "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(source.getAlerts().getFirst()).isSameAs(original);
  }

  @Test
  void missingOrClosedAlertCannotReceiveDeliveryEvidence() {
    var pending = pendingCase();
    assertThatThrownBy(() -> pending.markAlertSent(AFTER_GRACE))
        .isInstanceOf(IllegalStateException.class);
    var source = omittedCase();
    assertThatThrownBy(() -> source.markAlertSent(AFTER_GRACE))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> source.markAlertFailed("failure"))
        .isInstanceOf(IllegalStateException.class);
    var original = source.addAlert(AFTER_GRACE);
    source.close(AFTER_GRACE.plusSeconds(60));

    assertThatThrownBy(() -> source.markAlertSent(AFTER_GRACE.plusSeconds(120)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> source.markAlertFailed("failure"))
        .isInstanceOf(IllegalStateException.class);
    assertThat(source.getAlerts().getFirst()).isSameAs(original);
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
  void close_rejectsMissingOrBackwardTimeWithoutChangingState() {
    var value = omittedCase();
    assertThatThrownBy(() -> value.close(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> value.close(AFTER_GRACE.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(value.getStatus()).isEqualTo(OmissionCaseStatus.OMITTED);
    assertThat(value.getClosedAt()).isNull();
    var latest = AFTER_GRACE.plusSeconds(1800);
    value.escalate("still unresolved", latest);
    assertThatThrownBy(() -> value.close(latest.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(value.getStatus()).isEqualTo(OmissionCaseStatus.ESCALATED);
    assertThat(value.getClosedAt()).isNull();
    value.close(latest);
    assertThat(value.getClosedAt()).isEqualTo(latest);
    assertThat(value.getStatus()).isEqualTo(OmissionCaseStatus.CLOSED);
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

  @Test
  void invalidAlertTimeDoesNotMutateOmissionAndBoundaryTimeIsAccepted() {
    var omission = omittedCase();
    assertThatThrownBy(() -> omission.addAlert(AFTER_GRACE.minusNanos(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> omission.addAlert(null)).isInstanceOf(IllegalArgumentException.class);
    assertThat(omission.getAlerts()).isEmpty();
    assertThat(omission.getStatus()).isEqualTo(OmissionCaseStatus.OMITTED);
    var alert = omission.addAlert(AFTER_GRACE);
    assertThat(alert.getGeneratedAt()).isEqualTo(AFTER_GRACE);
    assertThat(omission.addAlert(AFTER_GRACE.plusSeconds(10))).isSameAs(alert);
    assertThat(alert.getGeneratedAt()).isEqualTo(AFTER_GRACE);
  }

  @Test
  void nullReminderTimeIsRejectedWithoutRecordingDelivery() {
    var omission = pendingCase();
    assertThatThrownBy(() -> omission.markReminderSent(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(omission.getReinforcedReminderSentAt()).isNull();
    omission.markReminderSent(START);
    omission.markReminderSent(START.plusSeconds(1));
    assertThat(omission.getReinforcedReminderSentAt()).isEqualTo(START);
  }

  @Test
  void pendingCaseRequiresSourceReferencesAndExplicitTiming() {
    var grace = GracePeriod.startingAt(START, Duration.ofMinutes(30));
    assertThatThrownBy(() -> new OmissionCase(null, "adult", "medication", START, grace))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OmissionCase("intake", " ", "medication", START, grace))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OmissionCase("intake", "adult", " ", START, grace))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OmissionCase("intake", "adult", "medication", null, grace))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OmissionCase("intake", "adult", "medication", START, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
