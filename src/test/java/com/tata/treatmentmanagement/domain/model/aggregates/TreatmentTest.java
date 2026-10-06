package com.tata.treatmentmanagement.domain.model.aggregates;

import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreatmentTest {
    @Test
    void incompleteTreatmentCannotActivate() {
        var treatment = Treatment.create("adult-1", "Control de presión", Instant.parse("2026-10-05T12:00:00Z"));
        assertEquals(TreatmentStatus.DRAFT, treatment.status());
        assertThrows(IllegalStateException.class, treatment::activate);
    }

    @Test
    void configuredTreatmentCanActivatePauseAndResumeWithoutLosingRegimen() {
        var treatment = Treatment.create("adult-1", "Control de presión", Instant.parse("2026-10-05T12:00:00Z"));
        treatment.configure(new TreatmentRegimen(
                "med-1",
                "1 comprimido",
                "DAILY",
                List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                "Con agua",
                10
        ));

        treatment.activate();
        assertEquals(TreatmentStatus.ACTIVE, treatment.status());

        treatment.pause();
        assertEquals(TreatmentStatus.PAUSED, treatment.status());

        treatment.resume();
        assertEquals(TreatmentStatus.ACTIVE, treatment.status());
        assertEquals("med-1", treatment.regimen().medicationId());
        assertEquals(List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)), treatment.regimen().scheduledTimes());
    }

    @Test
    void scheduleTimesAreCanonicalizedChronologicallyWithoutDuplicates() {
        var regimen = new TreatmentRegimen(
                "med-1",
                "1 comprimido",
                "DAILY",
                List.of(LocalTime.of(20, 0), LocalTime.of(8, 0), LocalTime.of(20, 0)),
                "Con agua",
                10
        );

        assertEquals(List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)), regimen.scheduledTimes());
    }

    @Test
    void regimenRequiresAtLeastOneScheduledTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TreatmentRegimen(
                        "med-1",
                        "1 comprimido",
                        "DAILY",
                        List.of(),
                        "Con agua",
                        10
                )
        );
    }
}
