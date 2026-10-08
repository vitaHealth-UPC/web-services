package com.tata.treatmentmanagement.application;

import com.tata.treatmentmanagement.application.internal.queryservices.MedicationCatalogQueryServiceImpl;
import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MedicationCatalogQueryServiceTest {
    @Test
    void joinsRegimensByMedicationAndKeepsUnconfiguredMedications() {
        var medications = mock(MedicationRepository.class);
        var treatments = mock(TreatmentRepository.class);
        var medication = Medication.register("adult-1", "Losartán", "50 mg", Instant.now());
        var unconfigured = Medication.register("adult-1", "Vitamina D", "Cápsula", Instant.now());
        var treatment = Treatment.create("adult-1", "Control", Instant.now());
        treatment.configure(new TreatmentRegimen(medication.id(), "1 comprimido", "DAILY",
                List.of(LocalTime.of(8, 0)), "Con agua", 10));
        treatment.activate();
        when(medications.findByOlderAdultId("adult-1")).thenReturn(List.of(medication, unconfigured));
        when(treatments.findByOlderAdultId("adult-1")).thenReturn(List.of(treatment));

        var result = new MedicationCatalogQueryServiceImpl(medications, treatments)
                .handle(new GetMyMedicationCatalogQuery("adult-1"));
        assertEquals(2, result.size());
        assertEquals("Losartán", result.getFirst().medication().name());
        assertEquals("1 comprimido", result.getFirst().treatments().getFirst().dose());
        assertTrue(result.get(1).treatments().isEmpty());
        verify(medications).findByOlderAdultId("adult-1");
        verify(treatments).findByOlderAdultId("adult-1");
        verifyNoMoreInteractions(medications, treatments);
    }
}
