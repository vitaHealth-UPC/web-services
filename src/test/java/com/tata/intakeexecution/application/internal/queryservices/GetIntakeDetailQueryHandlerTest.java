package com.tata.intakeexecution.application.internal.queryservices;

import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetIntakeDetailQueryHandlerTest {

    @Test
    void returnsMedicationDoseScheduleInstructionsAndStatus() {
        var intake = Intake.rehydrate(
                "intake-1",
                "treatment-1",
                "medication-1",
                "adult-1",
                new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua"),
                Instant.parse("2026-10-06T13:00:00Z"),
                IntakeStatus.LATE,
                Instant.parse("2026-10-05T12:00:00Z")
        );
        var handler = new GetIntakeDetailQueryHandler(new SingleIntakeRepository(intake));

        var result = handler.handle(" intake-1 ").orElseThrow();

        assertEquals("Losartán 50 mg", result.medicationName());
        assertEquals("1 comprimido", result.dose());
        assertEquals("Con agua", result.instructions());
        assertEquals(Instant.parse("2026-10-06T13:00:00Z"), result.scheduledAt());
        assertEquals(IntakeStatus.LATE, result.status());
    }

    @Test
    void returnsEmptyWhenIntakeDoesNotExist() {
        var handler = new GetIntakeDetailQueryHandler(new SingleIntakeRepository(null));

        assertTrue(handler.handle("missing").isEmpty());
    }

    @Test
    void rejectsBlankReference() {
        var handler = new GetIntakeDetailQueryHandler(new SingleIntakeRepository(null));

        assertThrows(IllegalArgumentException.class, () -> handler.handle(" "));
    }

    private static final class SingleIntakeRepository implements IntakeRepository {
        public java.util.List<Intake> findAgenda(String olderAdultId, java.time.Instant from, java.time.Instant to) { throw new UnsupportedOperationException(); }
        private final Intake intake;

        private SingleIntakeRepository(Intake intake) {
            this.intake = intake;
        }

        @Override
        public List<Intake> saveAll(List<Intake> intakes) {
            return intakes;
        }

        @Override
        public Optional<Intake> findById(String id) {
            return intake != null && intake.id().equals(id) ? Optional.of(intake) : Optional.empty();
        }

        @Override
        public List<Intake> findFutureByTreatmentId(String treatmentId, Instant from) {
            return List.of();
        }

        @Override
        public void deleteAll(List<Intake> intakes) {}

        @Override
        public Optional<Intake> findNextPendingByOlderAdultId(String olderAdultId, Instant from) {
            return Optional.empty();
        }
    }
}
