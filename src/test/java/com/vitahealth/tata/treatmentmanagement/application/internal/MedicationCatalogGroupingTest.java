package com.vitahealth.tata.treatmentmanagement.application.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.treatmentmanagement.application.internal.queryservices.GetMyMedicationCatalogQueryHandler;
import com.vitahealth.tata.treatmentmanagement.application.internal.queryservices.MedicationCatalogQueryServiceImpl;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks catalog association and repository read bounds with multiple medications. */
class MedicationCatalogGroupingTest {
  @Test
  void groupsConfiguredHistoryWithoutChangingMedicationOrderOrRepeatingReads() {
    var medications = mock(MedicationRepository.class);
    var treatments = mock(TreatmentRepository.class);
    var first = Medication.register("adult", "First", "50 mg", Instant.EPOCH);
    var second = Medication.register("adult", "Second", "10 mg", Instant.EPOCH);
    var configured = Treatment.create("adult", "Configured", Instant.EPOCH);
    configured.configure(
        new TreatmentRegimen(second.id(), "10 mg", "daily", List.of(LocalTime.NOON), "water", 10));
    var draft = Treatment.create("adult", "Draft", Instant.EPOCH);
    when(medications.findByOlderAdultId("adult")).thenReturn(List.of(first, second));
    when(treatments.findByOlderAdultId("adult")).thenReturn(List.of(draft, configured));
    var catalog =
        new MedicationCatalogQueryServiceImpl(
                new GetMyMedicationCatalogQueryHandler(medications, treatments))
            .handle(new GetMyMedicationCatalogQuery("adult"))
            .toOptional()
            .orElseThrow();
    assertEquals(
        List.of(first.id(), second.id()),
        catalog.stream().map(item -> item.medication().id()).toList());
    assertTrue(catalog.getFirst().treatments().isEmpty());
    assertEquals(configured.id(), catalog.get(1).treatments().getFirst().id());
    assertThrows(UnsupportedOperationException.class, () -> catalog.get(1).treatments().clear());
    verify(medications).findByOlderAdultId("adult");
    verify(treatments).findByOlderAdultId("adult");
    verifyNoMoreInteractions(medications, treatments);
  }
}
