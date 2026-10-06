package com.tata.intakeexecution.application.internal.commandservices;

import com.tata.intakeexecution.application.commands.GenerateIntakesCommand;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GenerateIntakesCommandHandlerTest {

    @Test
    void generatesFuturePendingIntakesForActiveTreatment() {
        var repository = new InMemoryIntakeRepository();
        var clock = Clock.fixed(Instant.parse("2026-10-06T07:00:00Z"), ZoneOffset.UTC);
        var handler = new GenerateIntakesCommandHandler(repository, clock);

        var result = handler.handle(command(true, List.of(LocalTime.of(8, 0), LocalTime.of(20, 0))));

        assertEquals(14, result.size());
        assertEquals(Instant.parse("2026-10-06T08:00:00Z"), result.getFirst().scheduledAt());
        assertEquals(IntakeStatus.PENDING, result.getFirst().status());
    }

    @Test
    void regenerationReplacesOnlyFuturePendingIntakes() {
        var repository = new InMemoryIntakeRepository();
        var clock = Clock.fixed(Instant.parse("2026-10-06T07:00:00Z"), ZoneOffset.UTC);
        var snapshot = new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua");

        repository.values.add(Intake.rehydrate(
                "confirmed-1", "treatment-1", "medication-1", "adult-1", snapshot,
                Instant.parse("2026-10-06T08:00:00Z"), IntakeStatus.CONFIRMED,
                Instant.parse("2026-10-05T12:00:00Z")
        ));
        repository.values.add(Intake.rehydrate(
                "pending-old", "treatment-1", "medication-1", "adult-1", snapshot,
                Instant.parse("2026-10-06T20:00:00Z"), IntakeStatus.PENDING,
                Instant.parse("2026-10-05T12:00:00Z")
        ));

        var handler = new GenerateIntakesCommandHandler(repository, clock);
        var result = handler.handle(command(true, List.of(LocalTime.of(8, 0), LocalTime.of(20, 0))));

        assertFalse(repository.values.stream().anyMatch(intake -> intake.id().equals("pending-old")));
        assertEquals(1, repository.values.stream().filter(intake -> intake.id().equals("confirmed-1")).count());
        assertFalse(result.stream().anyMatch(intake -> intake.scheduledAt().equals(Instant.parse("2026-10-06T08:00:00Z"))));
    }

    @Test
    void pausingScheduleRemovesFuturePendingIntakesWithoutTouchingDefinitiveOnes() {
        var repository = new InMemoryIntakeRepository();
        var clock = Clock.fixed(Instant.parse("2026-10-06T07:00:00Z"), ZoneOffset.UTC);
        var snapshot = new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua");

        repository.values.add(Intake.rehydrate(
                "pending-1", "treatment-1", "medication-1", "adult-1", snapshot,
                Instant.parse("2026-10-06T08:00:00Z"), IntakeStatus.PENDING,
                Instant.parse("2026-10-05T12:00:00Z")
        ));
        repository.values.add(Intake.rehydrate(
                "confirmed-1", "treatment-1", "medication-1", "adult-1", snapshot,
                Instant.parse("2026-10-06T20:00:00Z"), IntakeStatus.CONFIRMED,
                Instant.parse("2026-10-05T12:00:00Z")
        ));

        var handler = new GenerateIntakesCommandHandler(repository, clock);
        var result = handler.handle(command(false, List.of(LocalTime.of(8, 0), LocalTime.of(20, 0))));

        assertEquals(0, result.size());
        assertEquals(1, repository.values.size());
        assertEquals("confirmed-1", repository.values.getFirst().id());
    }

    private static GenerateIntakesCommand command(boolean active, List<LocalTime> scheduledTimes) {
        return new GenerateIntakesCommand(
                "treatment-1",
                "medication-1",
                "adult-1",
                "Losartán 50 mg",
                "1 comprimido",
                "Cada día",
                scheduledTimes,
                "Con agua",
                10,
                active
        );
    }

    private static final class InMemoryIntakeRepository implements IntakeRepository {
        private final List<Intake> values = new ArrayList<>();

        @Override
        public List<Intake> saveAll(List<Intake> intakes) {
            values.addAll(intakes);
            return intakes;
        }

        @Override
        public List<Intake> findFutureByTreatmentId(String treatmentId, Instant from) {
            return values.stream()
                    .filter(intake -> intake.treatmentId().equals(treatmentId))
                    .filter(intake -> !intake.scheduledAt().isBefore(from))
                    .sorted(Comparator.comparing(Intake::scheduledAt))
                    .toList();
        }

        @Override
        public void deleteAll(List<Intake> intakes) {
            var ids = intakes.stream().map(Intake::id).collect(java.util.stream.Collectors.toSet());
            values.removeIf(intake -> ids.contains(intake.id()));
        }

        @Override
        public Optional<Intake> findNextPendingByOlderAdultId(String olderAdultId, Instant from) {
            return values.stream()
                    .filter(intake -> intake.olderAdultId().equals(olderAdultId))
                    .filter(Intake::isPending)
                    .filter(intake -> !intake.scheduledAt().isBefore(from))
                    .min(Comparator.comparing(Intake::scheduledAt));
        }
    }
}
