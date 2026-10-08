package com.tata.treatmentmanagement.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.tata.treatmentmanagement.application.internal.fakes.InMemoryMedicationRepository;
import com.tata.treatmentmanagement.application.internal.fakes.InMemoryTreatmentRepository;
import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExtendActiveTreatmentSchedulesCommandHandlerTest {
    @Test
    void republishesOnlyActiveTreatmentsWithActiveMedication() {
        var treatments = new InMemoryTreatmentRepository();
        var medications = new InMemoryMedicationRepository();
        var events = new ArrayList<TreatmentScheduleChangedEvent>();
        var handler = new ExtendActiveTreatmentSchedulesCommandHandler(treatments, medications, events::add);

        var olderAdultId = "older-1";
        var activeMedication = medications.save(Medication.register(olderAdultId, "Losartan", "50 mg", Instant.now()));
        var inactiveMedication = medications.save(Medication.register(olderAdultId, "Paused", "10 mg", Instant.now()));
        inactiveMedication.deactivate();
        medications.save(inactiveMedication);

        var active = Treatment.create(olderAdultId, "Active", Instant.now());
        active.configure(new TreatmentRegimen(
                activeMedication.id(), "50 mg", "daily", List.of(LocalTime.of(8, 0)), "with water", 10));
        active.activate();
        treatments.save(active);

        var paused = Treatment.create(olderAdultId, "Paused", Instant.now());
        paused.configure(new TreatmentRegimen(
                activeMedication.id(), "50 mg", "daily", List.of(LocalTime.of(9, 0)), "with water", 10));
        paused.activate();
        paused.pause();
        treatments.save(paused);

        var orphan = Treatment.create(olderAdultId, "Orphan", Instant.now());
        orphan.configure(new TreatmentRegimen(
                inactiveMedication.id(), "10 mg", "daily", List.of(LocalTime.of(10, 0)), "none", 5));
        orphan.activate();
        treatments.save(orphan);

        assertEquals(1, handler.handle());
        assertEquals(1, events.size());
        assertEquals(active.id(), events.getFirst().treatmentId());
        assertEquals(true, events.getFirst().active());
    }
}
