package com.tata.treatmentmanagement.domain.model.aggregates;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

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
}
