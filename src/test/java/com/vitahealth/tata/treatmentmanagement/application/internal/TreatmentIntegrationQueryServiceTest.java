package com.vitahealth.tata.treatmentmanagement.application.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.treatmentmanagement.application.internal.queryservices.TreatmentIntegrationQueryService;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies integration projections stay scoped to the medication owner and active schedules. */
class TreatmentIntegrationQueryServiceTest {
  private final MedicationRepository medications = mock(MedicationRepository.class);
  private final TreatmentRepository treatments = mock(TreatmentRepository.class);
  private final TreatmentIntegrationQueryService queries =
      new TreatmentIntegrationQueryService(medications, treatments);
  private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

  @Test
  void countsOnlyActiveMatchingSchedulesWithoutReadingEveryTreatmentOfTheAdult() {
    var medication = Medication.register("adult", "Losartán", "Comprimido", NOW);
    when(medications.findById(medication.id())).thenReturn(Optional.of(medication));
    var first =
        configured("adult", medication.id(), List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)));
    var second = configured("adult", medication.id(), List.of(LocalTime.NOON));
    var paused = configured("adult", medication.id(), List.of(LocalTime.NOON));
    paused.pause();
    var foreign = configured("other-adult", medication.id(), List.of(LocalTime.NOON));
    var unrelated = configured("adult", "other-medication", List.of(LocalTime.NOON));
    var draft = Treatment.create("adult", "Borrador", NOW);
    when(treatments.findByMedicationId(medication.id()))
        .thenReturn(List.of(first, second, paused, foreign, unrelated, draft));
    assertEquals(Optional.of(3), queries.scheduledDailyOccurrences(medication.id()));
    verify(treatments).findByMedicationId(medication.id());
    verifyNoMoreInteractions(treatments);
  }

  @Test
  void absentAndInactiveMedicationsHaveNoScheduleEvidence() {
    when(medications.findById("missing")).thenReturn(Optional.empty());
    assertTrue(queries.scheduledDailyOccurrences("missing").isEmpty());
    var medication = Medication.register("adult", "Losartán", "Comprimido", NOW);
    medication.deactivate();
    when(medications.findById(medication.id())).thenReturn(Optional.of(medication));
    assertTrue(queries.scheduledDailyOccurrences(medication.id()).isEmpty());
    verifyNoInteractions(treatments);
  }

  @Test
  void activeMedicationWithoutSchedulesHasNoPositiveOccurrenceCount() {
    var medication = Medication.register("adult", "Losartán", "Comprimido", NOW);
    when(medications.findById(medication.id())).thenReturn(Optional.of(medication));
    when(treatments.findByMedicationId(medication.id())).thenReturn(List.of());
    assertTrue(queries.scheduledDailyOccurrences(medication.id()).isEmpty());
  }

  @Test
  void optionalLookupsPreserveAbsenceAndAggregateIdentity() {
    var medication = Medication.register("adult", "Losartán", "Comprimido", NOW);
    var treatment = Treatment.create("adult", "Presión", NOW);
    when(medications.findById(medication.id())).thenReturn(Optional.of(medication));
    when(treatments.findById(treatment.id())).thenReturn(Optional.of(treatment));
    assertEquals(medication.id(), queries.findMedication(medication.id()).orElseThrow().id());
    assertEquals(treatment.id(), queries.findTreatment(treatment.id()).orElseThrow().id());
    assertTrue(queries.findMedication("missing").isEmpty());
    assertTrue(queries.findTreatment("missing").isEmpty());
  }

  private static Treatment configured(String adult, String medication, List<LocalTime> times) {
    var treatment = Treatment.create(adult, "Tratamiento", NOW);
    treatment.configure(new TreatmentRegimen(medication, "1 comprimido", "DAILY", times, "", 0));
    treatment.activate();
    return treatment;
  }
}
