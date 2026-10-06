package com.tata.treatmentmanagement.application.internal;

import com.tata.treatmentmanagement.application.TreatmentApplicationException;
import com.tata.treatmentmanagement.application.internal.commandservices.TreatmentCommandServiceImpl;
import com.tata.treatmentmanagement.application.internal.queryservices.TreatmentQueryServiceImpl;
import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.tata.treatmentmanagement.domain.model.commands.CreateTreatmentCommand;
import com.tata.treatmentmanagement.domain.model.commands.DeactivateMedicationCommand;
import com.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.tata.treatmentmanagement.domain.model.commands.UpdateMedicationCommand;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreatmentAuthorizationTest {

    @Test
    void activeCareLinkIsRequiredToCreateTreatment() {
        var service = new TreatmentCommandServiceImpl(
                new InMemoryMedicationRepository(),
                new InMemoryTreatmentRepository(),
                (caregiverId, olderAdultId) -> false
        );

        var exception = assertThrows(
                TreatmentApplicationException.class,
                () -> service.createTreatment(
                        new CreateTreatmentCommand("caregiver-1", "adult-1", "Control de presión")
                )
        );

        assertEquals(TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED, exception.code());
    }

    @Test
    void activeCareLinkCreatesIncompleteTreatmentForOlderAdult() {
        var service = new TreatmentCommandServiceImpl(
                new InMemoryMedicationRepository(),
                new InMemoryTreatmentRepository(),
                (caregiverId, olderAdultId) ->
                        caregiverId.equals("caregiver-1") && olderAdultId.equals("adult-1")
        );

        var result = service.createTreatment(
                new CreateTreatmentCommand("caregiver-1", "adult-1", "Control de presión")
        );

        assertEquals("adult-1", result.olderAdultId());
        assertEquals(TreatmentStatus.INCOMPLETE, result.status());
    }

    @Test
    void activeCareLinkIsRequiredToRegisterMedication() {
        var service = new TreatmentCommandServiceImpl(
                new InMemoryMedicationRepository(),
                new InMemoryTreatmentRepository(),
                (caregiverId, olderAdultId) -> false
        );

        var exception = assertThrows(
                TreatmentApplicationException.class,
                () -> service.registerMedication(
                        new RegisterMedicationCommand("caregiver-1", "adult-1", "Losartán", "50 mg")
                )
        );

        assertEquals(TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED, exception.code());
    }

    @Test
    void medicationMutationAndDetailRejectCaregiverWithoutActiveLink() {
        var medications = new InMemoryMedicationRepository();
        var medication = Medication.register(
                "adult-1",
                "Losartán",
                "50 mg",
                Instant.parse("2026-10-05T12:00:00Z")
        );
        medications.save(medication);

        var service = new TreatmentCommandServiceImpl(
                medications,
                new InMemoryTreatmentRepository(),
                (caregiverId, olderAdultId) -> false
        );
        var queries = new TreatmentQueryServiceImpl(
                medications,
                new InMemoryTreatmentRepository(),
                (caregiverId, olderAdultId) -> false
        );

        assertEquals(
                TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED,
                assertThrows(
                        TreatmentApplicationException.class,
                        () -> service.updateMedication(
                                new UpdateMedicationCommand("caregiver-2", medication.id(), "Losartán", "100 mg")
                        )
                ).code()
        );
        assertEquals(
                TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED,
                assertThrows(
                        TreatmentApplicationException.class,
                        () -> service.deactivateMedication(
                                new DeactivateMedicationCommand("caregiver-2", medication.id())
                        )
                ).code()
        );
        assertEquals(
                TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED,
                assertThrows(
                        TreatmentApplicationException.class,
                        () -> queries.getMedication("caregiver-2", medication.id())
                ).code()
        );
    }

    @Test
    void treatmentDetailRejectsCaregiverWithoutActiveLink() {
        var treatments = new InMemoryTreatmentRepository();
        var treatment = Treatment.create(
                "adult-1",
                "Control de presión",
                Instant.parse("2026-10-05T12:00:00Z")
        );
        treatments.save(treatment);

        var queries = new TreatmentQueryServiceImpl(
                new InMemoryMedicationRepository(),
                treatments,
                (caregiverId, olderAdultId) -> false
        );

        var exception = assertThrows(
                TreatmentApplicationException.class,
                () -> queries.getTreatment("caregiver-2", treatment.id())
        );

        assertEquals(TreatmentApplicationException.Code.CARE_LINK_NOT_AUTHORIZED, exception.code());
    }

    private static final class InMemoryMedicationRepository implements MedicationRepository {
        private final Map<String, Medication> values = new HashMap<>();

        @Override
        public Medication save(Medication medication) {
            values.put(medication.id(), medication);
            return medication;
        }

        @Override
        public Optional<Medication> findById(String id) {
            return Optional.ofNullable(values.get(id));
        }
    }

    private static final class InMemoryTreatmentRepository implements TreatmentRepository {
        private final Map<String, Treatment> values = new HashMap<>();

        @Override
        public Treatment save(Treatment treatment) {
            values.put(treatment.id(), treatment);
            return treatment;
        }

        @Override
        public Optional<Treatment> findById(String id) {
            return Optional.ofNullable(values.get(id));
        }
    }
}
