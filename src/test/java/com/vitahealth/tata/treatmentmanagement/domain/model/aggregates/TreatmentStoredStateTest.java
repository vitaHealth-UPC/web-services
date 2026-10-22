package com.vitahealth.tata.treatmentmanagement.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies persistence reconstruction cannot invent a valid treatment from inconsistent state. */
class TreatmentStoredStateTest {
  @Test
  void activeAndPausedRowsRequirePersistedRegimenWhileDraftRemainsUnconfigured() {
    for (var status : List.of(TreatmentStatus.ACTIVE, TreatmentStatus.PAUSED)) {
      assertThrows(
          IllegalArgumentException.class,
          () -> Treatment.rehydrate("id", "adult", "Treatment", status, null, Instant.EPOCH));
    }
    assertNull(
        Treatment.rehydrate("id", "adult", "Treatment", TreatmentStatus.DRAFT, null, Instant.EPOCH)
            .regimen());
  }

  @Test
  void boundsRejectInvalidInputBeforeReplacingExistingRegimen() {
    var treatment = Treatment.create("adult", "x".repeat(160), Instant.EPOCH);
    var valid =
        new TreatmentRegimen("medication", "50 mg", "daily", List.of(LocalTime.NOON), "", 0);
    treatment.configure(valid);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            treatment.configure(
                new TreatmentRegimen(
                    "medication", "x".repeat(101), "daily", List.of(LocalTime.NOON), "", 0)));
    assertSame(valid, treatment.regimen());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TreatmentRegimen(
                "medication", "50 mg", "daily", List.of(LocalTime.NOON), "x".repeat(501), 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> Treatment.create("adult", "x".repeat(161), Instant.EPOCH));
  }
}
