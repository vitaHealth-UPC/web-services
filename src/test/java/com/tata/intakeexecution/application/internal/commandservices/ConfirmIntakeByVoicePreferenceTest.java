package com.tata.intakeexecution.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tata.intakeexecution.domain.model.commands.ConfirmIntakeByVoiceCommand;
import com.tata.intakeexecution.application.internal.IntakeApplicationException;
import com.tata.intakeexecution.application.internal.outboundservices.IVoiceRecognitionPort;
import com.tata.intakeexecution.application.models.VoiceConfirmationResult.VoiceConfirmationStatus;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
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

/** Voice confirmation follows the preference the user saved in Accessibility & Preferences. */
class ConfirmIntakeByVoicePreferenceTest {

    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-06T13:00:00Z");
    private static final Clock CLOCK = Clock.fixed(SCHEDULED_AT, ZoneOffset.UTC);

    private final Intake intake = Intake.rehydrate(
            "intake-1", "treatment-1", "medication-1", "adult-1",
            new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua"),
            SCHEDULED_AT, IntakeStatus.PENDING, Instant.parse("2026-10-05T12:00:00Z"));
    private final IntakeRepository repository = new OneIntakeRepository(intake);
    private final ConfirmIntakeCommandHandler confirm =
            new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK);

    private static ConfirmIntakeByVoiceCommand command() {
        return new ConfirmIntakeByVoiceCommand("intake-1", new byte[]{1, 2, 3}, "audio/wav", "es-419");
    }

    @Test
    void voiceTurnedOffIsRejectedWithoutCallingTheProvider() {
        var providerCalls = new AtomicInteger();
        IVoiceRecognitionPort voice = (audio, contentType, language) -> {
            providerCalls.incrementAndGet();
            return IVoiceRecognitionPort.VoiceRecognitionResult.recognized("Confirmo que tomé Losartán.", 0.99);
        };
        var handler = new ConfirmIntakeByVoiceCommandHandler(voice, repository, confirm, olderAdultId -> false);

        var exception = assertThrows(IntakeApplicationException.class, () -> handler.handle(command()));

        assertEquals(IntakeApplicationException.Code.VOICE_CONFIRMATION_DISABLED, exception.code());
        assertEquals(0, providerCalls.get());
    }

    @Test
    void thePreferenceIsAskedForTheOlderAdultOfTheIntake() {
        var asked = new java.util.ArrayList<String>();
        IVoiceRecognitionPort voice = (audio, contentType, language) ->
                IVoiceRecognitionPort.VoiceRecognitionResult.unrecognized(null, 0.0);
        var handler = new ConfirmIntakeByVoiceCommandHandler(voice, repository, confirm, olderAdultId -> {
            asked.add(olderAdultId);
            return true;
        });

        var result = handler.handle(command());

        assertEquals(List.of("adult-1"), asked);
        assertEquals(VoiceConfirmationStatus.NOT_RECOGNIZED, result.status());
    }

    private static final class OneIntakeRepository implements IntakeRepository {
        private Intake intake;

        private OneIntakeRepository(Intake intake) {
            this.intake = intake;
        }

        @Override
        public List<Intake> findAgenda(String olderAdultId, Instant from, Instant to) {
            return List.of();
        }

        @Override
        public List<Intake> saveAll(List<Intake> intakes) {
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
