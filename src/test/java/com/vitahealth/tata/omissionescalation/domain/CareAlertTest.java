package com.vitahealth.tata.omissionescalation.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.omissionescalation.domain.model.entities.CareAlert;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.assemblers.OmissionCasePersistenceAssembler;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.CareAlertPersistenceEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Protects retry evidence and rejects contradictory historical state without repairing it. */
class CareAlertTest {
  private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");

  @Test
  void retrySuccessClearsFailureAndSuccessfulEvidenceIsTerminal() {
    var alert = new CareAlert(NOW);
    var failed = alert.withFailedDelivery("push provider failure");
    var sent = failed.withSuccessfulDelivery(NOW.plusSeconds(60));
    assertEquals(AlertStatus.GENERATED, alert.getStatus());
    assertEquals(AlertStatus.FAILED, failed.getStatus());
    assertNull(sent.getFailureReason());
    assertEquals(NOW, sent.getGeneratedAt());
    assertThrows(IllegalStateException.class, () -> sent.withFailedDelivery("failure"));
    assertThrows(
        IllegalStateException.class, () -> sent.withSuccessfulDelivery(NOW.plusSeconds(120)));
    assertEquals(NOW.plusSeconds(60), sent.getSentAt());
  }

  @Test
  void invalidAttemptDoesNotMutateGeneratedEvidence() {
    assertThrows(IllegalArgumentException.class, () -> new CareAlert(null));
    var alert = new CareAlert(NOW);
    assertThrows(
        IllegalArgumentException.class, () -> alert.withSuccessfulDelivery(NOW.minusSeconds(1)));
    assertThrows(IllegalArgumentException.class, () -> alert.withSuccessfulDelivery(null));
    assertThrows(IllegalArgumentException.class, () -> alert.withFailedDelivery(" "));
    assertEquals(AlertStatus.GENERATED, alert.getStatus());
    assertNull(alert.getSentAt());
    assertNull(alert.getFailureReason());
  }

  @Test
  void restorationRejectsContradictionsAndRetainsValidHistory() {
    assertThrows(
        IllegalArgumentException.class,
        () -> CareAlert.rehydrate(1L, AlertStatus.SENT, NOW, null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> CareAlert.rehydrate(1L, AlertStatus.FAILED, NOW, NOW, "failure"));
    assertThrows(
        IllegalArgumentException.class,
        () -> CareAlert.rehydrate(1L, AlertStatus.GENERATED, NOW, null, "failure"));
    var stored = CareAlert.rehydrate(1L, AlertStatus.FAILED, NOW, null, " historical reason ");
    assertEquals(" historical reason ", stored.getFailureReason());
    assertEquals(1L, stored.getId());
    var accepted = stored.withSuccessfulDelivery(NOW.plusSeconds(60));
    assertEquals(stored.getId(), accepted.getId());
    assertEquals(stored.getGeneratedAt(), accepted.getGeneratedAt());
    assertEquals(AlertStatus.FAILED, stored.getStatus());
  }

  @Test
  void invalidPersistenceStateIsATechnicalFailure() {
    var row = new CareAlertPersistenceEntity();
    row.setId(1L);
    row.setStatus(AlertStatus.SENT);
    row.setGeneratedAt(NOW);
    var failure =
        assertThrows(
            IllegalStateException.class, () -> OmissionCasePersistenceAssembler.toDomain(row));
    assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    assertNull(row.getSentAt());
  }
}
