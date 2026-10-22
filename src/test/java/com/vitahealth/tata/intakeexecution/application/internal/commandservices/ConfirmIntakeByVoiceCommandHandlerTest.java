package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoiceRecognitionPort;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.IntakeConfirmationAccessPort;
import com.vitahealth.tata.intakeexecution.application.result.VoiceConfirmationResult.VoiceConfirmationStatus;
import com.vitahealth.tata.intakeexecution.application.result.VoiceRecognitionResult;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeByVoiceCommand;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeReportingCandidate;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.infrastructure.audio.WavVoiceAudioValidator;
import com.vitahealth.tata.intakeexecution.infrastructure.external.adapters.VoiceRecognitionAdapter;
import com.vitahealth.tata.intakeexecution.infrastructure.external.dto.SpeechToTextProviderResponse;
import com.vitahealth.tata.intakeexecution.support.WavAudioFixture;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
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
    var confirm =
        new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK, olderAdultId -> true);
    VoiceRecognitionPort voice =
        (audio, language) ->
            VoiceRecognitionResult.recognized(
                "Confirmo que tomé Losartán cincuenta miligramos.", 0.96);
    var handler =
        new ConfirmIntakeByVoiceCommandHandler(
            voice,
            repository,
            new ConfirmIntakeCommandServiceImpl(confirm),
            olderAdultId -> true,
            new WavVoiceAudioValidator(),
            mock(AuthenticatedIntakeConfirmationService.class),
            mock(IntakeConfirmationAccessPort.class));

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
    var confirm =
        new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK, olderAdultId -> true);
    VoiceRecognitionPort voice =
        (audio, language) -> VoiceRecognitionResult.recognized("Confirmo que tomé Losartán.", 0.30);
    var handler =
        new ConfirmIntakeByVoiceCommandHandler(
            voice,
            repository,
            new ConfirmIntakeCommandServiceImpl(confirm),
            olderAdultId -> true,
            new WavVoiceAudioValidator(),
            mock(AuthenticatedIntakeConfirmationService.class),
            mock(IntakeConfirmationAccessPort.class));

    var result = handler.handle(command());

    assertEquals(VoiceConfirmationStatus.NOT_VALIDATED, result.status());
    assertEquals(IntakeStatus.PENDING, repository.intake.status());
    assertEquals(0, repository.saveCalls);
  }

  @Test
  void questionsAndExplicitUncertaintyDoNotRecordEvidenceOrPublishConfirmation() {
    for (var phrase : List.of("¿Ya tomé mi medicamento?", "Creo que ya tomé Losartán")) {
      var repository = new SingleIntakeRepository(pendingIntake());
      var published = new AtomicInteger();
      var confirm =
          new ConfirmIntakeCommandHandler(
              repository, event -> published.incrementAndGet(), CLOCK, adult -> true);
      var handler =
          new ConfirmIntakeByVoiceCommandHandler(
              (audio, language) -> VoiceRecognitionResult.recognized(phrase, 0.99),
              repository,
              new ConfirmIntakeCommandServiceImpl(confirm),
              adult -> true,
              new WavVoiceAudioValidator(),
              mock(AuthenticatedIntakeConfirmationService.class),
              mock(IntakeConfirmationAccessPort.class));

      var result = handler.handle(command());

      assertEquals(VoiceConfirmationStatus.NOT_VALIDATED, result.status());
      assertEquals(IntakeStatus.PENDING, repository.intake.status());
      assertNull(repository.intake.confirmedAt());
      assertNull(repository.intake.confirmationChannel());
      assertEquals(0, repository.saveCalls);
      assertEquals(0, published.get());
    }
  }

  @Test
  void unrecognizedAudioDoesNotConfirm() {
    var repository = new SingleIntakeRepository(pendingIntake());
    var confirm =
        new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK, olderAdultId -> true);
    VoiceRecognitionPort voice =
        (audio, language) -> VoiceRecognitionResult.unrecognized(null, 0.0);
    var handler =
        new ConfirmIntakeByVoiceCommandHandler(
            voice,
            repository,
            new ConfirmIntakeCommandServiceImpl(confirm),
            olderAdultId -> true,
            new WavVoiceAudioValidator(),
            mock(AuthenticatedIntakeConfirmationService.class),
            mock(IntakeConfirmationAccessPort.class));

    var result = handler.handle(command());

    assertEquals(VoiceConfirmationStatus.NOT_RECOGNIZED, result.status());
    assertEquals(IntakeStatus.PENDING, repository.intake.status());
    assertEquals(0, repository.saveCalls);
  }

  @Test
  void providerFailureLeavesIntakePending() {
    var repository = new SingleIntakeRepository(pendingIntake());
    var confirm =
        new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK, olderAdultId -> true);
    VoiceRecognitionPort voice =
        (audio, language) -> VoiceRecognitionResult.unavailable("provider timeout");
    var handler =
        new ConfirmIntakeByVoiceCommandHandler(
            voice,
            repository,
            new ConfirmIntakeCommandServiceImpl(confirm),
            olderAdultId -> true,
            new WavVoiceAudioValidator(),
            mock(AuthenticatedIntakeConfirmationService.class),
            mock(IntakeConfirmationAccessPort.class));

    var result = handler.handle(command());

    assertEquals(VoiceConfirmationStatus.PROVIDER_UNAVAILABLE, result.status());
    assertEquals(IntakeStatus.PENDING, repository.intake.status());
    assertEquals(0, repository.saveCalls);
  }

  @Test
  void retryAfterExistingConfirmationDoesNotCallSpeechProviderOrPersistAgain() {
    var repository = new SingleIntakeRepository(pendingIntake());
    var confirm =
        new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK, olderAdultId -> true);
    confirm.handle(
        new com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand(
            "intake-1", ConfirmationChannel.TOUCH));
    int savesBeforeRetry = repository.saveCalls;
    AtomicInteger recognitionCalls = new AtomicInteger();
    VoiceRecognitionPort voice =
        (audio, language) -> {
          recognitionCalls.incrementAndGet();
          return VoiceRecognitionResult.recognized("Confirmo que tomé Losartán.", 0.99);
        };
    var handler =
        new ConfirmIntakeByVoiceCommandHandler(
            voice,
            repository,
            new ConfirmIntakeCommandServiceImpl(confirm),
            olderAdultId -> true,
            new WavVoiceAudioValidator(),
            mock(AuthenticatedIntakeConfirmationService.class),
            mock(IntakeConfirmationAccessPort.class));

    var result = handler.handle(command());

    assertEquals(VoiceConfirmationStatus.ALREADY_CONFIRMED, result.status());
    assertEquals(0, recognitionCalls.get());
    assertEquals(savesBeforeRetry, repository.saveCalls);
    assertEquals(ConfirmationChannel.TOUCH, result.intake().confirmationChannel());
  }

  @Test
  void aTouchConfirmationDuringRecognitionIsReturnedAsAnIdempotentReplay() {
    var repository = new SingleIntakeRepository(pendingIntake());
    var confirm =
        new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK, olderAdultId -> true);
    VoiceRecognitionPort voice =
        (audio, language) -> {
          confirm.handle(
              new com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand(
                  "intake-1", ConfirmationChannel.TOUCH));
          return VoiceRecognitionResult.recognized("Confirmo que tomé Losartán", 0.99);
        };
    var result =
        new ConfirmIntakeByVoiceCommandHandler(
                voice,
                repository,
                new ConfirmIntakeCommandServiceImpl(confirm),
                olderAdultId -> true,
                new WavVoiceAudioValidator(),
                mock(AuthenticatedIntakeConfirmationService.class),
                mock(IntakeConfirmationAccessPort.class))
            .handle(command());
    assertEquals(VoiceConfirmationStatus.ALREADY_CONFIRMED, result.status());
    assertEquals(true, result.intake().alreadyConfirmed());
    assertEquals(ConfirmationChannel.TOUCH, result.intake().confirmationChannel());
    assertEquals(1, repository.saveCalls);
  }

  @Test
  void excessiveProviderConfidenceCannotTriggerAClinicalConfirmation() {
    var repository = new SingleIntakeRepository(pendingIntake());
    var confirm = new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK, adult -> true);
    var voice =
        new VoiceRecognitionAdapter(
            (audio, language) ->
                new SpeechToTextProviderResponse("Confirmo que tomé Losartán", 1.5));
    var handler =
        new ConfirmIntakeByVoiceCommandHandler(
            voice,
            repository,
            new ConfirmIntakeCommandServiceImpl(confirm),
            adult -> true,
            new WavVoiceAudioValidator(),
            mock(AuthenticatedIntakeConfirmationService.class),
            mock(IntakeConfirmationAccessPort.class));
    var result = handler.handle(command());
    assertEquals(VoiceConfirmationStatus.PROVIDER_UNAVAILABLE, result.status());
    assertEquals(IntakeStatus.PENDING, repository.intake.status());
    assertEquals(0, repository.saveCalls);
  }

  @Test
  void invalidAudioOrLanguageIsRejectedBeforeProviderDeliveryOrConfirmation() {
    var invalidCommands =
        List.of(
            new ConfirmIntakeByVoiceCommand(
                "intake-1", new byte[] {1, 2, 3}, "audio/wav", "es-419"),
            new ConfirmIntakeByVoiceCommand(
                "intake-1", WavAudioFixture.shortRecording(), "audio/wav", "fr-FR"));
    for (var command : invalidCommands) {
      var repository = new SingleIntakeRepository(pendingIntake());
      var confirm = new ConfirmIntakeCommandHandler(repository, event -> {}, CLOCK, adult -> true);
      var calls = new AtomicInteger();
      VoiceRecognitionPort provider =
          (audio, language) -> {
            calls.incrementAndGet();
            return VoiceRecognitionResult.recognized("Confirmo que tomé Losartán", 0.99);
          };
      var handler =
          new ConfirmIntakeByVoiceCommandHandler(
              provider,
              repository,
              new ConfirmIntakeCommandServiceImpl(confirm),
              adult -> true,
              new WavVoiceAudioValidator(),
              mock(AuthenticatedIntakeConfirmationService.class),
              mock(IntakeConfirmationAccessPort.class));
      var outcome =
          new ConfirmIntakeByVoiceCommandServiceImpl(
                  handler, mock(IntakeConfirmationAccessPort.class))
              .handle(command);
      var failure = assertInstanceOf(Result.Failure.class, outcome);
      assertEquals("VALIDATION_ERROR", ((ApplicationError) failure.error()).code());
      assertEquals(0, calls.get());
      assertEquals(0, repository.saveCalls);
      assertEquals(IntakeStatus.PENDING, repository.intake.status());
    }
  }

  private static ConfirmIntakeByVoiceCommand command() {
    return new ConfirmIntakeByVoiceCommand(
        "intake-1", WavAudioFixture.shortRecording(), "audio/wav", "es-419");
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
        Instant.parse("2026-10-05T12:00:00Z"));
  }

  private static final class SingleIntakeRepository implements IntakeRepository {
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
