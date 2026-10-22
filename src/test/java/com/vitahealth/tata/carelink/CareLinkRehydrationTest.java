package com.vitahealth.tata.carelink;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.model.valueobjects.Consent;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Checks restored lifecycle evidence without renewing codes or changing domain timestamps. */
class CareLinkRehydrationTest {
  private final Instant created = Instant.parse("2026-10-13T12:00:00Z");
  private final Instant used = created.plusSeconds(1);
  private final Instant decision = created.plusSeconds(2);
  private final Instant confirmation = created.plusSeconds(3);
  private final Instant withdrawal = created.plusSeconds(4);

  private CareLink restore(
      CareLinkStatus status,
      Instant use,
      boolean consent,
      Instant decisionAt,
      Instant confirmedAt,
      Instant updatedAt) {
    return CareLink.rehydrate(
        "link",
        "caregiver",
        "adult",
        status,
        LinkingCode.rehydrate("TATA-4821", created.plusSeconds(15), use),
        Consent.rehydrate(consent, decisionAt),
        confirmedAt,
        created,
        updatedAt);
  }

  @Test
  void restoresEveryLegitimateLifecycleSnapshotIncludingUnconfirmedApproval() {
    assertEquals(
        CareLinkStatus.PENDING,
        restore(CareLinkStatus.PENDING, null, false, null, null, created).status());
    assertEquals(
        CareLinkStatus.AWAITING_CONSENT,
        restore(CareLinkStatus.AWAITING_CONSENT, used, false, null, null, used).status());
    assertEquals(
        CareLinkStatus.AWAITING_CONSENT,
        restore(CareLinkStatus.AWAITING_CONSENT, used, true, decision, null, decision).status());
    assertTrue(
        restore(CareLinkStatus.CONFIRMED, used, true, decision, confirmation, confirmation)
            .isActive());
    assertFalse(restore(CareLinkStatus.REVOKED, used, false, decision, null, decision).isActive());
    var revoked =
        restore(CareLinkStatus.REVOKED, used, false, withdrawal, confirmation, withdrawal);
    assertEquals(confirmation, revoked.confirmedAt());
    assertEquals(withdrawal, revoked.consent().recordedAt());
  }

  @Test
  void pendingCannotContainUsedCodeOrRecordedConsentOrConfirmation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.PENDING, used, false, null, null, used));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.PENDING, used, true, decision, null, decision));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.PENDING, used, true, decision, confirmation, confirmation));
  }

  @Test
  void awaitingConsentRequiresUseAndCannotContainRejectionOrConfirmation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.AWAITING_CONSENT, null, false, null, null, created));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.AWAITING_CONSENT, used, false, decision, null, decision));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            restore(
                CareLinkStatus.AWAITING_CONSENT, used, true, decision, confirmation, confirmation));
  }

  @Test
  void confirmedRequiresApprovalAndConfirmationAfterDecision() {
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.CONFIRMED, used, false, decision, confirmation, confirmation));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.CONFIRMED, used, true, decision, null, decision));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.CONFIRMED, used, true, confirmation, decision, confirmation));
  }

  @Test
  void revokedRequiresRejectionAndWithdrawalCannotPrecedeHistoricalConfirmation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.REVOKED, used, true, decision, confirmation, withdrawal));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.REVOKED, used, false, null, null, used));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.REVOKED, used, false, decision, confirmation, withdrawal));
  }

  @Test
  void evidenceCannotPrecedeCreationOrExceedLatestUpdate() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            restore(
                CareLinkStatus.AWAITING_CONSENT, created.minusSeconds(1), false, null, null, used));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.AWAITING_CONSENT, used, false, null, null, created));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.CONFIRMED, used, true, decision, confirmation, decision));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(CareLinkStatus.PENDING, null, false, null, null, created.minusSeconds(1)));
  }

  @Test
  void expiredHistoricalCodesDoNotInvalidateConfirmedOrRejectedEvidence() {
    var lateDecision = created.plusSeconds(60);
    assertTrue(
        restore(CareLinkStatus.CONFIRMED, used, true, lateDecision, lateDecision, lateDecision)
            .isActive());
    assertEquals(
        CareLinkStatus.REVOKED,
        restore(CareLinkStatus.REVOKED, used, false, lateDecision, null, lateDecision).status());
  }
}
