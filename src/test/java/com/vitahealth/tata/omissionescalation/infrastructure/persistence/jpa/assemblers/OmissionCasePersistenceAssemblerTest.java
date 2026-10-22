package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.assemblers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.embeddables.EscalationLevelPersistenceEmbeddable;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.CareAlertPersistenceEntity;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.EscalationRecordPersistenceEntity;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Distinguishes corrupted stored child evidence from invalid incoming business commands. */
class OmissionCasePersistenceAssemblerTest {
  private static final Instant OCCURRED_AT = Instant.parse("2026-10-18T12:00:00Z");

  @ParameterizedTest
  @MethodSource("invalidEscalations")
  void rejectsMalformedEscalationWithoutRepairingStoredFields(
      Consumer<EscalationRecordPersistenceEntity> corrupt) {
    var entity = escalation();
    corrupt.accept(entity);
    var level = entity.getLevel();
    var reason = entity.getReason();
    var time = entity.getTriggeredAt();
    var id = entity.getId();

    var failure =
        assertThrows(
            IllegalStateException.class, () -> OmissionCasePersistenceAssembler.toDomain(entity));

    assertNotNull(failure.getCause());
    assertEquals(level, entity.getLevel());
    assertEquals(reason, entity.getReason());
    assertEquals(time, entity.getTriggeredAt());
    assertEquals(id, entity.getId());
  }

  static Stream<Consumer<EscalationRecordPersistenceEntity>> invalidEscalations() {
    return Stream.of(
        entity -> entity.setId(null),
        entity -> entity.setLevel(null),
        entity -> entity.setLevel(new EscalationLevelPersistenceEmbeddable(0)),
        entity -> entity.setLevel(new EscalationLevelPersistenceEmbeddable(4)),
        entity -> entity.setReason(" "),
        entity -> entity.setTriggeredAt(null));
  }

  @ParameterizedTest
  @MethodSource("invalidAlerts")
  void rejectsMalformedAlertWithoutRepairingDeliveryEvidence(
      Consumer<CareAlertPersistenceEntity> corrupt) {
    var entity = alert();
    corrupt.accept(entity);
    var status = entity.getStatus();
    var sentAt = entity.getSentAt();
    var reason = entity.getFailureReason();

    var failure =
        assertThrows(
            IllegalStateException.class, () -> OmissionCasePersistenceAssembler.toDomain(entity));

    assertNotNull(failure.getCause());
    assertEquals(status, entity.getStatus());
    assertEquals(sentAt, entity.getSentAt());
    assertEquals(reason, entity.getFailureReason());
  }

  static Stream<Consumer<CareAlertPersistenceEntity>> invalidAlerts() {
    return Stream.of(
        entity -> entity.setId(null),
        entity -> entity.setStatus(null),
        entity -> entity.setGeneratedAt(null),
        entity -> entity.setSentAt(null),
        entity -> entity.setSentAt(OCCURRED_AT.minusSeconds(1)),
        entity -> entity.setFailureReason("failed after success"));
  }

  @Test
  void restoresOriginalChildIdentitiesAndOccurrenceTimes() {
    var escalation = OmissionCasePersistenceAssembler.toDomain(escalation());
    var alert = OmissionCasePersistenceAssembler.toDomain(alert());

    assertEquals(7L, escalation.getId());
    assertEquals(1, escalation.getLevel().value());
    assertEquals("unresolved omission", escalation.getReason());
    assertEquals(OCCURRED_AT, escalation.getTriggeredAt());
    assertEquals(8L, alert.getId());
    assertEquals(AlertStatus.SENT, alert.getStatus());
    assertEquals(OCCURRED_AT, alert.getGeneratedAt());
    assertEquals(OCCURRED_AT.plusSeconds(60), alert.getSentAt());
  }

  private static EscalationRecordPersistenceEntity escalation() {
    var entity = new EscalationRecordPersistenceEntity();
    entity.setId(7L);
    entity.setLevel(new EscalationLevelPersistenceEmbeddable(1));
    entity.setReason("unresolved omission");
    entity.setTriggeredAt(OCCURRED_AT);
    return entity;
  }

  private static CareAlertPersistenceEntity alert() {
    var entity = new CareAlertPersistenceEntity();
    entity.setId(8L);
    entity.setStatus(AlertStatus.SENT);
    entity.setGeneratedAt(OCCURRED_AT);
    entity.setSentAt(OCCURRED_AT.plusSeconds(60));
    return entity;
  }
}
