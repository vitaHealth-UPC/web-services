package com.tata.treatmentmanagement.application.internal;

import com.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.tata.treatmentmanagement.application.internal.commandservices.TreatmentCommandServiceImpl;
import com.tata.treatmentmanagement.application.internal.fakes.InMemoryMedicationRepository;
import com.tata.treatmentmanagement.application.internal.fakes.InMemoryTreatmentRepository;
import com.tata.treatmentmanagement.application.internal.queryservices.TreatmentQueryServiceImpl;
import com.tata.treatmentmanagement.domain.model.commands.ChangeTreatmentStatusCommand;
import com.tata.treatmentmanagement.domain.model.commands.ConfigureTreatmentCommand;
import com.tata.treatmentmanagement.domain.model.commands.CreateTreatmentCommand;
import com.tata.treatmentmanagement.domain.model.commands.DeactivateMedicationCommand;
import com.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.tata.treatmentmanagement.domain.model.commands.UpdateMedicationCommand;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps treatments, medications and the published schedule consistent with each other. */
class TreatmentConsistencyTest {
    private static final String CAREGIVER = "caregiver-1";
    private static final String OLDER_ADULT = "adult-1";

    private final List<TreatmentScheduleChangedEvent> events = new ArrayList<>();
    private TreatmentCommandServiceImpl commands;
    private TreatmentQueryServiceImpl queries;

    @BeforeEach
    void setUp() {
        var medications = new InMemoryMedicationRepository();
        var treatments = new InMemoryTreatmentRepository();
        commands = new TreatmentCommandServiceImpl(medications, treatments, (caregiverId, olderAdultId) -> true, events::add);
        queries = new TreatmentQueryServiceImpl(medications, treatments, (caregiverId, olderAdultId) -> true);
    }

    private String medication(String name) {
        return commands.registerMedication(new RegisterMedicationCommand(CAREGIVER, OLDER_ADULT, name, "50 mg")).id();
    }

    private String activeTreatment(String medicationId) {
        var treatmentId = commands.createTreatment(new CreateTreatmentCommand(CAREGIVER, OLDER_ADULT, "Presión")).id();
        commands.configureTreatment(new ConfigureTreatmentCommand(
                CAREGIVER, treatmentId, medicationId, "1 comprimido", "DAILY",
                List.of(LocalTime.of(8, 0)), "Con agua", 10));
        commands.activate(new ChangeTreatmentStatusCommand(CAREGIVER, treatmentId));
        events.clear();
        return treatmentId;
    }

    @Test
    void deactivatingTheMedicationPausesTheActiveTreatmentAndTellsIntakeExecution() {
        var medicationId = medication("Losartán");
        var treatmentId = activeTreatment(medicationId);

        commands.deactivateMedication(new DeactivateMedicationCommand(CAREGIVER, medicationId));

        assertEquals(TreatmentStatus.PAUSED, queries.getTreatment(CAREGIVER, treatmentId).status());
        assertEquals(1, events.size());
        assertFalse(events.getFirst().active());
        assertEquals(treatmentId, events.getFirst().treatmentId());
    }

    @Test
    void deactivatingAMedicationNobodyUsesPublishesNothing() {
        var medicationId = medication("Losartán");

        commands.deactivateMedication(new DeactivateMedicationCommand(CAREGIVER, medicationId));

        assertTrue(events.isEmpty());
    }

    @Test
    void renamingTheMedicationRepublishesTheScheduleWithTheNewName() {
        var medicationId = medication("Losartán");
        activeTreatment(medicationId);

        commands.updateMedication(new UpdateMedicationCommand(CAREGIVER, medicationId, "Losartán Potásico", "100 mg"));

        assertEquals(1, events.size());
        assertEquals("Losartán Potásico", events.getFirst().medicationName());
        assertTrue(events.getFirst().active());
    }

    @Test
    void aTreatmentWhoseMedicationWasDeactivatedCannotBeResumed() {
        var medicationId = medication("Losartán");
        var treatmentId = activeTreatment(medicationId);
        commands.deactivateMedication(new DeactivateMedicationCommand(CAREGIVER, medicationId));

        var exception = assertThrows(
                TreatmentApplicationException.class,
                () -> commands.resume(new ChangeTreatmentStatusCommand(CAREGIVER, treatmentId))
        );

        assertEquals(TreatmentApplicationException.Code.MEDICATION_INACTIVE, exception.code());
    }

    @Test
    void aDraftTreatmentCannotBeActivatedWithoutRegimen() {
        var treatmentId = commands.createTreatment(new CreateTreatmentCommand(CAREGIVER, OLDER_ADULT, "Presión")).id();

        var exception = assertThrows(
                TreatmentApplicationException.class,
                () -> commands.activate(new ChangeTreatmentStatusCommand(CAREGIVER, treatmentId))
        );

        assertEquals(TreatmentApplicationException.Code.INCOMPLETE_TREATMENT, exception.code());
        assertTrue(events.isEmpty());
    }

    @Test
    void configuringAnInactiveMedicationIsRejected() {
        var medicationId = medication("Losartán");
        commands.deactivateMedication(new DeactivateMedicationCommand(CAREGIVER, medicationId));
        var treatmentId = commands.createTreatment(new CreateTreatmentCommand(CAREGIVER, OLDER_ADULT, "Presión")).id();

        var exception = assertThrows(
                TreatmentApplicationException.class,
                () -> commands.configureTreatment(new ConfigureTreatmentCommand(
                        CAREGIVER, treatmentId, medicationId, "1 comprimido", "DAILY",
                        List.of(LocalTime.of(8, 0)), "", 0))
        );

        assertEquals(TreatmentApplicationException.Code.MEDICATION_INACTIVE, exception.code());
    }

    @Test
    void listsOnlyTheMedicationsAndTreatmentsOfTheRequestedOlderAdult() {
        medication("Losartán");
        medication("Atorvastatina");
        commands.registerMedication(new RegisterMedicationCommand(CAREGIVER, "adult-2", "Metformina", "850 mg"));
        commands.createTreatment(new CreateTreatmentCommand(CAREGIVER, OLDER_ADULT, "Presión"));
        commands.createTreatment(new CreateTreatmentCommand(CAREGIVER, "adult-2", "Diabetes"));

        assertEquals(2, queries.listMedications(CAREGIVER, OLDER_ADULT).size());
        assertEquals(1, queries.listTreatments(CAREGIVER, OLDER_ADULT).size());
        assertEquals("Diabetes", queries.listTreatments(CAREGIVER, "adult-2").getFirst().name());
    }

    @Test
    void listingRequiresAnActiveCareLink() {
        var denied = new TreatmentQueryServiceImpl(
                new InMemoryMedicationRepository(), new InMemoryTreatmentRepository(), (caregiverId, olderAdultId) -> false);

        assertEquals(
                TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED,
                assertThrows(TreatmentApplicationException.class, () -> denied.listMedications("x", OLDER_ADULT)).code());
        assertEquals(
                TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED,
                assertThrows(TreatmentApplicationException.class, () -> denied.listTreatments("x", OLDER_ADULT)).code());
    }
}
