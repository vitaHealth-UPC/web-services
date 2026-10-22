package com.vitahealth.tata.treatmentmanagement.application.internal;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Prevents schedule publication from mixing medication and adult evidence. */
class TreatmentScheduleChangedEventAssemblerTest {
  @Test
  void foreignAdultMedicationCannotBecomeAScheduleSnapshot() {
    var medication = Medication.register("another-adult", "Losartan", "50 mg", Instant.EPOCH);
    var treatment = Treatment.create("adult", "Treatment", Instant.EPOCH);
    treatment.configure(
        new TreatmentRegimen(
            medication.id(), "50 mg", "daily", List.of(LocalTime.NOON), "with water", 10));
    assertThrows(
        IllegalArgumentException.class,
        () -> TreatmentScheduleChangedEventAssembler.toEvent(treatment, medication));
  }

  @Test
  void snapshotPreservesPausedStateAndConfiguredEvidence() {
    var medication = Medication.register("adult", "Losartan", "50 mg", Instant.EPOCH);
    var treatment = Treatment.create("adult", "Treatment", Instant.EPOCH);
    treatment.configure(
        new TreatmentRegimen(
            medication.id(), "50 mg", "daily", List.of(LocalTime.NOON), "with water", 10));
    treatment.activate();
    treatment.pause();
    var event = TreatmentScheduleChangedEventAssembler.toEvent(treatment, medication);
    assertFalse(event.active());
    assertEquals(treatment.id(), event.treatmentId());
    assertEquals("Losartan", event.medicationName());
    assertEquals(List.of(LocalTime.NOON), event.scheduledTimes());
    assertThrows(UnsupportedOperationException.class, () -> event.scheduledTimes().clear());
  }
}
