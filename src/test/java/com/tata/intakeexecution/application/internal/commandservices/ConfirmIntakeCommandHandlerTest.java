package com.tata.intakeexecution.application.internal.commandservices;

import com.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.tata.intakeexecution.application.internal.IntakeApplicationException;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmIntakeCommandHandlerTest {

    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-06T13:00:00Z");
    private static final Clock ON_TIME_CLOCK = Clock.fixed(SCHEDULED_AT, ZoneOffset.UTC);

    @Test
    void confirmsPendingIntakeAndPersistsTransition() {
        var repository = new SingleIntakeRepository(pendingIntake());
        var handler = new ConfirmIntakeCommandHandler(repository, event -> {}, ON_TIME_CLOCK);

        var result = handler.handle(new ConfirmIntakeCommand(" intake-1 ", ConfirmationChannel.TOUCH));

        assertEquals(IntakeStatus.CONFIRMED, result.status());
        assertEquals(1, repository.saveCalls);
    }

    @Test
    void retryFromAnotherChannelDoesNotCreateAnotherTransition() {
        var repository = new SingleIntakeRepository(pendingIntake());
        var events = new java.util.ArrayList<Object>();
        var handler = new ConfirmIntakeCommandHandler(repository, events::add, ON_TIME_CLOCK);

        handler.handle(new ConfirmIntakeCommand("intake-1", ConfirmationChannel.TOUCH));
        var retry = handler.handle(new ConfirmIntakeCommand("intake-1", ConfirmationChannel.VOICE));

        assertEquals(IntakeStatus.CONFIRMED, retry.status());
        assertEquals(1, repository.saveCalls);
        assertEquals(1, events.size());
        var event = (com.tata.intakeexecution.domain.model.events.IntakeConfirmed) events.getFirst();
        assertEquals("medication-1", event.medicationId());
        assertEquals("adult-1", event.olderAdultId());
        assertEquals(repository.intake.confirmedAt(), event.confirmedAt());
        assertEquals(ConfirmationChannel.TOUCH, repository.intake.confirmationChannel());
    }

    @Test
    void classifiesConfirmationAfterScheduleAsLate() {
        var repository = new SingleIntakeRepository(pendingIntake());
        var lateClock = Clock.fixed(SCHEDULED_AT.plusSeconds(60), ZoneOffset.UTC);
        var handler = new ConfirmIntakeCommandHandler(repository, event -> {}, lateClock);

        var result = handler.handle(new ConfirmIntakeCommand("intake-1", ConfirmationChannel.TOUCH));

        assertEquals(IntakeStatus.LATE, result.status());
        assertEquals(SCHEDULED_AT.plusSeconds(60), result.confirmedAt());
    }

    @Test
    void returnsNotFoundWhenIntakeDoesNotExist() {
        var handler = new ConfirmIntakeCommandHandler(
                new SingleIntakeRepository(null),
                event -> {},
                ON_TIME_CLOCK
        );

        var exception = assertThrows(
                IntakeApplicationException.class,
                () -> handler.handle(new ConfirmIntakeCommand("missing", ConfirmationChannel.TOUCH))
        );

        assertEquals(IntakeApplicationException.Code.INTAKE_NOT_FOUND, exception.code());
    }

    @Test
    void rejectsConfirmationAfterOmission() {
        var repository = new SingleIntakeRepository(Intake.rehydrate(
                "intake-1",
                "treatment-1",
                "medication-1",
                "adult-1",
                new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua"),
                SCHEDULED_AT,
                IntakeStatus.OMITTED,
                Instant.parse("2026-10-05T12:00:00Z")
        ));
        var handler = new ConfirmIntakeCommandHandler(repository, event -> {}, ON_TIME_CLOCK);

        var exception = assertThrows(
                IntakeApplicationException.class,
                () -> handler.handle(new ConfirmIntakeCommand("intake-1", ConfirmationChannel.VOICE))
        );

        assertEquals(IntakeApplicationException.Code.INTAKE_NOT_CONFIRMABLE, exception.code());
        assertEquals(0, repository.saveCalls);
    }

    private static Intake pendingIntake() {
        return Intake.rehydrate(
                "intake-1",
                "treatment-1",
                "medication-1",
                "adult-1",
                new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua"),
                SCHEDULED_AT,
                IntakeStatus.PENDING,
                Instant.parse("2026-10-05T12:00:00Z")
        );
    }

    private static final class SingleIntakeRepository implements IntakeRepository {
        public java.util.List<Intake> findAgenda(
                String olderAdultId,
                java.time.Instant from,
                java.time.Instant to
        ) {
            throw new UnsupportedOperationException();
        }

        private Intake intake;
        private int saveCalls;

        private SingleIntakeRepository(Intake intake) {
            this.intake = intake;
        }

        @Override
        public List<Intake> saveAll(List<Intake> intakes) {
            saveCalls++;
            if (!intakes.isEmpty()) {
                intake = intakes.getFirst();
            }
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
        public void deleteAll(List<Intake> intakes) {
        }

        @Override
        public Optional<Intake> findNextPendingByOlderAdultId(String olderAdultId, Instant from) {
            return Optional.empty();
        }
    }
}
