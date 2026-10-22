package com.vitahealth.tata.carelink.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.services.CareLinkConfirmationPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Checks relationship transitions, immutable evidence and rejection without partial mutation. */
class CareLinkTest {
  private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");

  @Test
  void adultWithdrawalPreservesConfirmationAndCannotBeRepeatedOrPerformedByAnotherActor() {
    var link =
        CareLink.createPending(
            "caregiver", "adult", LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(15)), NOW);
    link.accept("TATA-4821", NOW.plusSeconds(10));
    link.registerConsent(true, NOW.plusSeconds(20));
    link.confirm(NOW.plusSeconds(20));
    assertThrows(
        IllegalArgumentException.class,
        () -> link.withdrawConsent("other-adult", NOW.plusSeconds(30)));
    assertThrows(
        IllegalArgumentException.class, () -> link.withdrawConsent("adult", NOW.plusSeconds(19)));
    assertTrue(link.isActive());
    assertTrue(link.withdrawConsent("adult", NOW.plusSeconds(30)));
    assertFalse(link.isActive());
    assertEquals(CareLinkStatus.REVOKED, link.status());
    assertEquals(NOW.plusSeconds(20), link.confirmedAt());
    assertEquals(NOW, link.createdAt());
    assertEquals(NOW.plusSeconds(10), link.linkingCode().usedAt());
    assertFalse(link.withdrawConsent("adult", NOW.plusSeconds(40)));
    assertEquals(NOW.plusSeconds(30), link.updatedAt());
  }

  @Test
  void externallyConsumedSnapshotCannotChangeTheOwningAggregate() {
    var code = LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(15));
    var link = CareLink.createPending("caregiver", "adult", code, NOW);
    var externalSnapshot = link.linkingCode().useAt(NOW.plusSeconds(5));
    assertNotNull(externalSnapshot.usedAt());
    assertNull(link.linkingCode().usedAt());
    assertEquals(CareLinkStatus.PENDING, link.status());
    link.accept("TATA-4821", NOW.plusSeconds(10));
    assertNull(code.usedAt());
    assertEquals(NOW.plusSeconds(10), link.linkingCode().usedAt());
  }

  @Test
  void invalidConfirmationTimeLeavesGrantedOnboardingStateIntact() {
    var link =
        CareLink.createPending(
            "caregiver", "adult", LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(15)), NOW);
    link.accept("TATA-4821", NOW.plusSeconds(10));
    link.registerConsent(true, NOW.plusSeconds(20));
    assertThrows(NullPointerException.class, () -> link.confirm(null));
    assertThrows(IllegalArgumentException.class, () -> link.confirm(NOW.plusSeconds(19)));
    assertEquals(CareLinkStatus.AWAITING_CONSENT, link.status());
    assertNull(link.confirmedAt());
    assertEquals(NOW.plusSeconds(20), link.updatedAt());
    assertTrue(link.consent().isGranted());
    assertFalse(new CareLinkConfirmationPolicy().canConfirm(link, NOW.plusSeconds(19)));
  }

  @Test
  void consentCannotPrecedeCodeAcceptance() {
    var link =
        CareLink.createPending(
            "caregiver", "adult", LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(15)), NOW);
    link.accept("TATA-4821", NOW.plusSeconds(10));
    assertThrows(
        IllegalArgumentException.class, () -> link.registerConsent(false, NOW.plusSeconds(9)));
    assertEquals(CareLinkStatus.AWAITING_CONSENT, link.status());
    assertNull(link.consent().recordedAt());
    assertEquals(NOW.plusSeconds(10), link.updatedAt());
  }

  @Test
  void validCodeAndConsentConfirmTheCareLink() {
    var code = LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(15));
    var link = CareLink.createPending("caregiver-1", "adult-1", code, NOW);

    link.accept("TATA-4821", NOW.plusSeconds(30));
    link.registerConsent(true, NOW.plusSeconds(60));

    assertTrue(new CareLinkConfirmationPolicy().canConfirm(link, NOW.plusSeconds(60)));

    link.confirm(NOW.plusSeconds(60));

    assertEquals(CareLinkStatus.CONFIRMED, link.status());
    assertTrue(link.isActive());
    assertNotNull(link.confirmedAt());
  }

  @Test
  void expiredCodeCannotBeAccepted() {
    var code = LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(1));
    var link = CareLink.createPending("caregiver-1", "adult-1", code, NOW);

    assertThrows(
        IllegalStateException.class,
        () -> link.accept("TATA-4821", NOW.plus(Duration.ofMinutes(2))));
    assertEquals(CareLinkStatus.PENDING, link.status());
  }

  @Test
  void codeCannotBeUsedTwice() {
    var code = LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(15));
    var link = CareLink.createPending("caregiver-1", "adult-1", code, NOW);

    link.accept("TATA-4821", NOW.plusSeconds(10));

    assertThrows(IllegalStateException.class, () -> link.accept("TATA-4821", NOW.plusSeconds(20)));
  }

  @Test
  void rejectedConsentRevokesTheCareLink() {
    var code = LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(15));
    var link = CareLink.createPending("caregiver-1", "adult-1", code, NOW);

    link.accept("TATA-4821", NOW.plusSeconds(10));
    link.registerConsent(false, NOW.plusSeconds(20));

    assertEquals(CareLinkStatus.REVOKED, link.status());
    assertFalse(link.isActive());
  }
}
