package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Verifies snapshot input boundaries before persistence can truncate or reject clinical text. */
class MedicationSnapshotTest {
  @Test
  void normalizesTextAndRetainsExactlyTheSupportedMaximumLengths() {
    var snapshot =
        new MedicationSnapshot(
            " " + "N".repeat(160) + " ", " " + "D".repeat(100) + " ", " " + "I".repeat(500) + " ");
    assertEquals(160, snapshot.name().length());
    assertEquals(100, snapshot.dose().length());
    assertEquals(500, snapshot.instructions().length());
    assertEquals("", new MedicationSnapshot("Medicamento", "1 comprimido", null).instructions());
  }

  @Test
  void rejectsEachOverlongFieldInsteadOfTruncatingItsValue() {
    assertThrows(
        IllegalArgumentException.class, () -> new MedicationSnapshot("N".repeat(161), "D", ""));
    assertThrows(
        IllegalArgumentException.class, () -> new MedicationSnapshot("N", "D".repeat(101), ""));
    assertThrows(
        IllegalArgumentException.class, () -> new MedicationSnapshot("N", "D", "I".repeat(501)));
  }

  @Test
  void requiresNameAndDoseBeforeCapturingEvidence() {
    assertThrows(IllegalArgumentException.class, () -> new MedicationSnapshot(null, "D", ""));
    assertThrows(IllegalArgumentException.class, () -> new MedicationSnapshot(" ", "D", ""));
    assertThrows(IllegalArgumentException.class, () -> new MedicationSnapshot("N", null, ""));
    assertThrows(IllegalArgumentException.class, () -> new MedicationSnapshot("N", " ", ""));
  }
}
