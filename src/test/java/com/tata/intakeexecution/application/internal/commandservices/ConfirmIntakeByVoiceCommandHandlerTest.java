package com.tata.intakeexecution.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tata.intakeexecution.application.commands.ConfirmIntakeByVoiceCommand;
import com.tata.intakeexecution.application.internal.outboundservices.IVoiceRecognitionPort;
import com.tata.intakeexecution.application.models.VoiceConfirmationResult.VoiceConfirmationStatus;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConfirmIntakeByVoiceCommandHandlerTest {

    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-06T13:00:00Z");
    private static final Clock CLOCK = Clock.fixed(SCHEDULED_AT, ZoneOffset.UTC);

    @Test
    void validRecognizedPhraseConfirmsUsingVoiceChannel() {
        var repository = new SingleIntakeRepository(pendingIntake());
        var confirm = new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK);
        IVoiceRecognitionPort voice = (audio, contentType, language) ->
                IVoiceRecognitionPort.VoiceRecognitionResult.recognized(
                        "Confirmo que tomé Losartán cincuenta miligramos.",
                        0.96
                );
        var handler = new ConfirmIntakeByVoiceCommandHandler(voice, repository, confirm);

        var result = handler.handle(command());

        assertEquals(VoiceConfirmationStatus.CONFIRMED, result.status());
        assertNotNull(result.intake());
        assertEquals(IntakeStatus.CONFIRMED, result.intake().status());
        assertEquals(ConfirmationChannel.VOICE, result.intake().confirmationChannel());
        assertEquals(1, repository.saveCalls);
    }

    @Test
    void lowConfidencePhraseDoesNotConfirm() {
        var repository = new SingleIntakeRepository(pendingIntake());
        var confirm = new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK);
        IVoiceRecognitionPort voice = (audio, contentType, language) ->
                IVoiceRecognitionPort.VoiceRecognitionResult.recognized(
                        "Confirmo que tomé Losartán.",
                        0.30
                );
        var handler = new ConfirmIntakeByVoiceCommandHandler(voice, repository, confirm);

        var result = handler.handle(command());

        assertEquals(VoiceConfirmationStatus.NOT_VALIDATED, result.status());
        assertEquals(IntakeStatus.PENDING, repository.intake.status());
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void unrecognizedAudioDoesNotConfirm() {
        var repository = new SingleIntakeRepository(pendingIntake());
        var confirm = new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK);
        IVoiceRecognitionPort voice = (audio, contentType, language) ->
                IVoiceRecognitionPort.VoiceRecognitionResult.unrecognized(null, 0.0);
        var handler = new ConfirmIntakeByVoiceCommandHandler(voice, repository, confirm);

        var result = handler.handle(command());

        assertEquals(VoiceConfirmationStatus.NOT_RECOGNIZED, result.status());
        assertEquals(IntakeStatus.PENDING, repository.intake.status());
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void providerFailureLeavesIntakePending() {
        var repository = new SingleIntakeRepository(pendingIntake());
        var confirm = new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK);
        IVoiceRecognitionPort voice = (audio, contentType, language) ->
                IVoiceRecognitionPort.VoiceRecognitionResult.unavailable("provider timeout");
        var handler = new ConfirmIntakeByVoiceCommandHandler(voice, repository, confirm);

        var result = handler.handle(command());

        assertEquals(VoiceConfirmationStatus.PROVIDER_UNAVAILABLE, result.status());
        assertEquals(IntakeStatus.PENDING, repository.intake.status());
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void retryAfterExistingConfirmationDoesNotCallSpeechProviderOrPersistAgain() {
        var repository = new SingleIntakeRepository(pendingIntake());
        var confirm = new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK);
        confirm.handle(new com.tata.intakeexecution.application.commands.ConfirmIntakeCommand(
                "intake-1",
                ConfirmationChannel.TOUCH
        ));
        int savesBeforeRetry = repository.saveCalls;
        AtomicInteger recognitionCalls = new AtomicInteger();
        IVoiceRecognitionPort voice = (audio, contentType, language) -> {
            recognitionCalls.incrementAndGet();
            return IVoiceRecognitionPort.VoiceRecognitionResult.recognized(
                    "Confirmo que tomé Losartán.",
                    0.99
            );
        };
        var handler = new ConfirmIntakeByVoiceCommandHandler(voice, repository, confirm);

        var result = handler.handle(command());

        assertEquals(VoiceConfirmationStatus.ALREADY_CONFIRMED, result.status());
        assertEquals(0, recognitionCalls.get());
        assertEquals(savesBeforeRetry, repository.saveCalls);
        assertEquals(ConfirmationChannel.TOUCH, result.intake().confirmationChannel());
    }

    private static ConfirmIntakeByVoiceCommand command() {
        return new ConfirmIntakeByVoiceCommand(
                "intake-1",
                new byte[]{1, 2, 3},
                "audio/wav",
                "es-419"
        );
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
        private Intake intake;
        private int saveCalls;

        private SingleIntakeRepository(Intake intake) {
            this.intake = intake;
        }

        @Override
        public List<Intake> findAgenda(String olderAdultId, Instant from, Instant to) {
            return List.of();
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
            return intake != null && intake.id().equals(id)
                    ? Optional.of(intake)
                    : Optional.empty();
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
