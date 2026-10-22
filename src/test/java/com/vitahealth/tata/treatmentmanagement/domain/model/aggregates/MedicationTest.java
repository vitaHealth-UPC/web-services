package com.vitahealth.tata.treatmentmanagement.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Verifies atomic medication changes and persistence-compatible description bounds. */
class MedicationTest {
  @Test
  void rejectedUpdatePreservesBothOriginalValues() {
    var medication = Medication.register("adult-1", "Losartán", "50 mg", Instant.now());
    assertThrows(IllegalArgumentException.class, () -> medication.update("Metformina", " "));
    assertEquals("Losartán", medication.name());
    assertEquals("50 mg", medication.presentation());
  }

  @Test
  void validUpdateNormalizesBothValues() {
    var medication = Medication.register("adult-1", "Losartán", "50 mg", Instant.now());
    medication.update(" Metformina ", " 850 mg ");
    assertEquals("Metformina", medication.name());
    assertEquals("850 mg", medication.presentation());
  }

  @Test
  void storageBoundsApplyToRegistrationReconstructionAndAtomicUpdates() {
    var now = Instant.parse("2026-10-14T12:00:00Z");
    var medication = Medication.register("adult", "x".repeat(160), "y".repeat(160), now);
    assertEquals(160, medication.name().length());
    assertThrows(
        IllegalArgumentException.class,
        () -> Medication.register("adult", "x".repeat(161), "presentation", now));
    assertThrows(
        IllegalArgumentException.class,
        () -> Medication.rehydrate("id", "adult", "name", "x".repeat(161), true, now));
    assertThrows(
        IllegalArgumentException.class, () -> medication.update("Changed", "x".repeat(161)));
    assertEquals("x".repeat(160), medication.name());
    assertEquals("y".repeat(160), medication.presentation());
    assertEquals(now, medication.createdAt());
  }

  @Test
  void inactiveMedicationRejectsEditsWithoutChangingItsDescription() {
    var medication = Medication.register("adult", "Losartan", "50 mg", Instant.EPOCH);
    medication.deactivate();
    assertThrows(IllegalStateException.class, () -> medication.update("Changed", "Changed"));
    assertEquals("Losartan", medication.name());
    assertEquals("50 mg", medication.presentation());
  }
}
