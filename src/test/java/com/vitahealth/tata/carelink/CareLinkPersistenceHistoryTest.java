package com.vitahealth.tata.carelink;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.CareLinkPersistenceEntity;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Ensures managed updates preserve immutable issuance and irreversible lifecycle evidence. */
class CareLinkPersistenceHistoryTest {
  private final Instant now = Instant.parse("2026-10-13T12:00:00Z");

  private CareLink link() {
    return CareLink.createPending(
        "caregiver", "adult", LinkingCode.issue("TATA-4821", now, Duration.ofMinutes(15)), now);
  }

  private CareLinkPersistenceEntity row(CareLink link) {
    return new CareLinkPersistenceEntity(
        link.id(),
        link.caregiverId(),
        link.olderAdultId(),
        link.status(),
        link.linkingCode().value(),
        link.linkingCode().expiresAt(),
        link.linkingCode().usedAt(),
        link.consent().isGranted(),
        link.consent().recordedAt(),
        link.confirmedAt(),
        link.createdAt(),
        link.updatedAt());
  }

  @Test
  void acceptanceConfirmationAndWithdrawalKeepOriginalEvidence() {
    var link = link();
    var stored = row(link);
    link.accept("TATA-4821", now.plusSeconds(1));
    stored.updateFrom(row(link));
    link.registerConsent(true, now.plusSeconds(2));
    link.confirm(now.plusSeconds(2));
    stored.updateFrom(row(link));
    link.withdrawConsent("adult", now.plusSeconds(3));
    stored.updateFrom(row(link));
    assertEquals(now, stored.getCreatedAt());
    assertEquals(now.plusSeconds(1), stored.getCodeUsedAt());
    assertEquals(now.plusSeconds(2), stored.getConfirmedAt());
    assertFalse(stored.isConsentGranted());
  }

  @Test
  void revokedSnapshotCannotBeReactivatedByAnOlderConfirmedCopy() {
    var link = link();
    link.accept("TATA-4821", now.plusSeconds(1));
    link.registerConsent(true, now.plusSeconds(2));
    link.confirm(now.plusSeconds(2));
    var old = row(link);
    link.withdrawConsent("adult", now.plusSeconds(2));
    var stored = row(link);
    assertThrows(IllegalArgumentException.class, () -> stored.updateFrom(old));
    assertFalse(stored.isConsentGranted());
    assertEquals(link.status(), stored.getStatus());
  }

  @Test
  void usedCodeCannotBeErasedByAnOldPendingCopy() {
    var link = link();
    var old = row(link);
    link.accept("TATA-4821", now);
    var stored = row(link);
    assertThrows(IllegalArgumentException.class, () -> stored.updateFrom(old));
    assertEquals(now, stored.getCodeUsedAt());
  }

  @Test
  void snapshotCannotReplaceIssuanceExpirationOrCreationTime() {
    var link = link();
    var stored = row(link);
    var altered =
        new CareLinkPersistenceEntity(
            link.id(),
            link.caregiverId(),
            link.olderAdultId(),
            link.status(),
            link.linkingCode().value(),
            link.linkingCode().expiresAt().plusSeconds(1),
            null,
            false,
            null,
            null,
            now,
            now);
    assertThrows(IllegalArgumentException.class, () -> stored.updateFrom(altered));
    assertEquals(link.linkingCode().expiresAt(), stored.getCodeExpiresAt());
  }
}
