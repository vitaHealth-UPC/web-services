package com.vitahealth.tata.intakeexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.assemblers.IntakePersistenceAssembler;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakePersistenceEntity;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Ensures persistence mapping preserves evidence and never repairs inconsistent stored state. */
class IntakeStoredEvidencePersistenceTest {
  private static final Instant AT = Instant.parse("2026-10-15T12:00:00Z");

  @Test
  void incompleteStoredConfirmationFailsWithoutMutatingSource() {
    for (var status : List.of(IntakeStatus.CONFIRMED, IntakeStatus.LATE)) {
      var entity = entity(status);
      entity.setConfirmation(AT, null);
      assertThrows(IllegalStateException.class, () -> IntakePersistenceAssembler.toDomain(entity));
      assertEquals(status, entity.getStatus());
      assertEquals(AT, entity.getConfirmedAt());
      assertNull(entity.getConfirmationChannel());
    }
  }

  @Test
  void unconfirmedStoredStatusCannotSilentlyDiscardConfirmation() {
    for (var status : List.of(IntakeStatus.PENDING, IntakeStatus.OMITTED)) {
      var entity = entity(status);
      entity.setConfirmation(AT, ConfirmationChannel.TOUCH);
      assertThrows(IllegalStateException.class, () -> IntakePersistenceAssembler.toDomain(entity));
      assertEquals(AT, entity.getConfirmedAt());
      assertEquals(ConfirmationChannel.TOUCH, entity.getConfirmationChannel());
    }
  }

  @Test
  void completeEvidenceRoundTripsWithOriginalHandoffMarker() {
    var entity = entity(IntakeStatus.LATE);
    entity.setConfirmation(AT.plusSeconds(60), ConfirmationChannel.VOICE);
    entity.setUnconfirmedReportedAt(AT.plusSeconds(30));
    var restored = IntakePersistenceAssembler.toEntity(IntakePersistenceAssembler.toDomain(entity));
    assertEquals(entity.getId(), restored.getId());
    assertEquals(entity.getStatus(), restored.getStatus());
    assertEquals(entity.getCreatedAt(), restored.getCreatedAt());
    assertEquals(entity.getConfirmedAt(), restored.getConfirmedAt());
    assertEquals(entity.getConfirmationChannel(), restored.getConfirmationChannel());
    assertEquals(entity.getUnconfirmedReportedAt(), restored.getUnconfirmedReportedAt());
  }

  private static IntakePersistenceEntity entity(IntakeStatus status) {
    return new IntakePersistenceEntity(
        "intake",
        "treatment",
        "medication",
        "adult",
        "Losartán",
        "1 comprimido",
        "",
        AT,
        status,
        AT.minusSeconds(60));
  }
}
