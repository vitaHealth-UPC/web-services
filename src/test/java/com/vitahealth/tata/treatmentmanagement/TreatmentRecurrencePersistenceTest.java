package com.vitahealth.tata.treatmentmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRecurrence;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.infrastructure.persistence.jpa.assemblers.TreatmentPersistenceAssembler;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies recurrence metadata survives mapping without guessing legacy anchors. */
class TreatmentRecurrencePersistenceTest {
  @Test
  void preservesExplicitIntervalAnchorAndZoneAcrossPersistenceMapping() {
    var recurrence =
        new TreatmentRecurrence(
            TreatmentRecurrence.Mode.FIXED_INTERVAL,
            "America/Bogota",
            Instant.parse("2026-10-21T13:00:00.123456Z"));
    var treatment = Treatment.create("adult", "Presión", Instant.EPOCH);
    treatment.configure(
        new TreatmentRegimen(
            "medication",
            "1 comprimido",
            "cada 12 horas",
            List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            "",
            10,
            recurrence));
    var restored =
        TreatmentPersistenceAssembler.toDomain(TreatmentPersistenceAssembler.toEntity(treatment));
    assertEquals(treatment.regimen(), restored.regimen());
  }

  @Test
  void legacyConstructorRetainsUtcLocalTimes() {
    var regimen = new TreatmentRegimen("medication", "1", "DAILY", List.of(LocalTime.NOON), "", 0);
    assertEquals(TreatmentRecurrence.legacyUtc(), regimen.recurrence());
  }

  @Test
  void rejectsPartialMetadataAsInputValidationAndPreservesAbsentLegacyMetadata() {
    assertEquals(
        TreatmentRecurrence.legacyUtc(), TreatmentRecurrence.fromMetadata(null, null, null));
    assertThrows(
        IllegalArgumentException.class, () -> TreatmentRecurrence.fromMetadata(null, "UTC", null));
  }

  @Test
  void rejectsInconsistentModesAndUnsupportedIntervalFrequency() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new TreatmentRecurrence(TreatmentRecurrence.Mode.FIXED_INTERVAL, "UTC", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new TreatmentRecurrence(TreatmentRecurrence.Mode.LOCAL_TIMES, "UTC", Instant.EPOCH));
    assertThrows(
        IllegalArgumentException.class,
        () -> new TreatmentRecurrence(TreatmentRecurrence.Mode.LOCAL_TIMES, "+05:00", null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TreatmentRecurrence(
                TreatmentRecurrence.Mode.FIXED_INTERVAL, "UTC", Instant.EPOCH.plusNanos(1)));
    var interval =
        new TreatmentRecurrence(TreatmentRecurrence.Mode.FIXED_INTERVAL, "UTC", Instant.EPOCH);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TreatmentRegimen(
                "medication", "1", "DAILY", List.of(LocalTime.NOON), "", 0, interval));
  }
}
