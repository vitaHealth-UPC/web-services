package com.vitahealth.tata.treatmentmanagement;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.assemblers.TreatmentPersistenceAssembler;
import com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.entities.TreatmentPersistenceEntity;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Detects incomplete persisted regimen evidence instead of inventing defaults or discarding it. */
class TreatmentStoredRegimenPersistenceTest {
  @Test
  void missingConfiguredReminderIsNotReplacedByZero() {
    assertThrows(
        IllegalStateException.class,
        () ->
            TreatmentPersistenceAssembler.toDomain(
                entity("medication", "1 comprimido", "DAILY", List.of(LocalTime.NOON), null)));
    var configured =
        TreatmentPersistenceAssembler.toDomain(
            entity("medication", "1 comprimido", "DAILY", List.of(LocalTime.NOON), 0));
    assertEquals(0, configured.regimen().reminderLeadMinutes());
  }

  @Test
  void partialRegimenWithoutMedicationIsNotDiscarded() {
    for (var partial :
        List.of(
            entity(null, "1 comprimido", null, List.of(), null),
            entity(null, null, "DAILY", List.of(), null),
            entity(null, null, null, List.of(LocalTime.NOON), null),
            entity(null, null, null, List.of(), 0))) {
      assertThrows(
          IllegalStateException.class, () -> TreatmentPersistenceAssembler.toDomain(partial));
    }
  }

  @Test
  void emptyDraftPreservesIdentityAndAbsenceOfRegimen() {
    var draft = TreatmentPersistenceAssembler.toDomain(entity(null, null, null, List.of(), null));
    assertEquals("treatment", draft.id());
    assertNull(draft.regimen());
    assertEquals(Instant.EPOCH, draft.createdAt());
  }

  private static TreatmentPersistenceEntity entity(
      String medication, String dose, String frequency, List<LocalTime> times, Integer reminder) {
    return new TreatmentPersistenceEntity(
        "treatment",
        "adult",
        "Presión",
        TreatmentStatus.DRAFT,
        medication,
        dose,
        frequency,
        times,
        null,
        reminder,
        Instant.EPOCH);
  }
}
