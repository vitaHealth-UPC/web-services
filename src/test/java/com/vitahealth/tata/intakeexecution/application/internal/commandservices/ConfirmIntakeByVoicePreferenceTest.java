package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoiceRecognitionPort;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.IntakeConfirmationAccessPort;
import com.vitahealth.tata.intakeexecution.application.result.VoiceConfirmationResult.VoiceConfirmationStatus;
import com.vitahealth.tata.intakeexecution.application.result.VoiceRecognitionResult;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeByVoiceCommand;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeReportingCandidate;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.infrastructure.audio.WavVoiceAudioValidator;
import com.vitahealth.tata.intakeexecution.support.WavAudioFixture;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Voice confirmation follows the preference the user saved in Accessibility & Preferences. */
class ConfirmIntakeByVoicePreferenceTest {

  private static final Instant SCHEDULED_AT = Instant.parse("2026-10-06T13:00:00Z");
  private static final Clock CLOCK = Clock.fixed(SCHEDULED_AT, ZoneOffset.UTC);

  private final Intake intake =
      Intake.rehydrate(
          "intake-1",
          "treatment-1",
          "medication-1",
          "adult-1",
          new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua"),
          SCHEDULED_AT,
          IntakeStatus.PENDING,
          Instant.parse("2026-10-05T12:00:00Z"));
  private final IntakeRepository repository = new OneIntakeRepository(intake);
  private final ConfirmIntakeCommandHandler confirm =
      new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK, olderAdultId -> true);

  private static ConfirmIntakeByVoiceCommand command() {
    return new ConfirmIntakeByVoiceCommand(
        "intake-1", WavAudioFixture.shortRecording(), "audio/wav", "es-419");
  }

  @Test
  void voiceTurnedOffIsRejectedWithoutCallingTheProvider() {
    var providerCalls = new AtomicInteger();
    VoiceRecognitionPort voice =
        (audio, language) -> {
          providerCalls.incrementAndGet();
          return VoiceRecognitionResult.recognized("Confirmo que tomé Losartán.", 0.99);
        };
    var handler =
        new ConfirmIntakeByVoiceCommandHandler(
            voice,
            repository,
            new ConfirmIntakeCommandServiceImpl(confirm),
            olderAdultId -> false,
            new WavVoiceAudioValidator(),
            mock(AuthenticatedIntakeConfirmationService.class),
            mock(IntakeConfirmationAccessPort.class));

    var exception = assertThrows(IntakeApplicationException.class, () -> handler.handle(command()));

    assertEquals(IntakeApplicationException.Code.VOICE_CONFIRMATION_DISABLED, exception.code());
    assertEquals(0, providerCalls.get());
  }

  @Test
  void thePreferenceIsAskedForTheOlderAdultOfTheIntake() {
    var asked = new java.util.ArrayList<String>();
    VoiceRecognitionPort voice =
        (audio, language) -> VoiceRecognitionResult.unrecognized(null, 0.0);
    var handler =
        new ConfirmIntakeByVoiceCommandHandler(
            voice,
            repository,
            new ConfirmIntakeCommandServiceImpl(confirm),
            olderAdultId -> {
              asked.add(olderAdultId);
              return true;
            },
            new WavVoiceAudioValidator(),
            mock(AuthenticatedIntakeConfirmationService.class),
            mock(IntakeConfirmationAccessPort.class));

    var result = handler.handle(command());

    assertEquals(List.of("adult-1"), asked);
    assertEquals(VoiceConfirmationStatus.NOT_RECOGNIZED, result.status());
  }

  @Test
  void disablingVoiceDuringRecognitionPreventsTheFinalLockedTransition() {
    var allowed = new AtomicBoolean(true);
    var confirming =
        new ConfirmIntakeCommandHandler(
            repository, event -> {}, CLOCK, olderAdultId -> allowed.get());
    VoiceRecognitionPort voice =
        (audio, language) -> {
          allowed.set(false);
          return VoiceRecognitionResult.recognized("Confirmo que tomé Losartán", 0.99);
        };
    var handler =
        new ConfirmIntakeByVoiceCommandHandler(
            voice,
            repository,
            new ConfirmIntakeCommandServiceImpl(confirming),
            olderAdultId -> allowed.get(),
            new WavVoiceAudioValidator(),
            mock(AuthenticatedIntakeConfirmationService.class),
            mock(IntakeConfirmationAccessPort.class));
    var failure = assertThrows(IntakeApplicationException.class, () -> handler.handle(command()));
    assertEquals(IntakeApplicationException.Code.VOICE_CONFIRMATION_DISABLED, failure.code());
    assertEquals(IntakeStatus.PENDING, intake.status());
    assertEquals(null, intake.confirmedAt());
  }

  private static final class OneIntakeRepository implements IntakeRepository {
    /** Single-threaded fixture; persistence tests verify row locking. */
    @Override
    public Optional<Intake> findByIdForConfirmation(String id) {
      return findById(id);
    }

    @Override
    public List<IntakeReportingCandidate> findUnreportedPendingDueAfter(
        Instant cutoff, IntakeReportingCandidate after, int limit) {
      throw new UnsupportedOperationException("omission reporting is outside this fixture");
    }

    @Override
    public List<String> findOlderAdultIdsWithIntakes(
        Instant from, Instant to, String after, int limit) {
      throw new UnsupportedOperationException("participant discovery is outside this fixture");
    }

    /** Single-threaded fixture; database serialization is verified by integration tests. */
    @Override
    public List<Intake> findFutureByTreatmentIdForUpdate(String treatmentId, Instant from) {
      throw new UnsupportedOperationException(
          "schedule reconciliation is outside this test fixture");
    }

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
    public void deleteAll(List<Intake> intakes) {}

    @Override
    public Optional<Intake> findNextPendingByOlderAdultId(String olderAdultId, Instant from) {
      return Optional.empty();
    }
  }
}
