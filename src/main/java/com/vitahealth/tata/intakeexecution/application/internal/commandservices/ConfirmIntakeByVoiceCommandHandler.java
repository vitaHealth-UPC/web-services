package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.commandservices.ConfirmIntakeCommandService;
import com.vitahealth.tata.intakeexecution.application.internal.assemblers.IntakeResultFromAggregateAssembler;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.IntakeConfirmationAccessPort;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoiceAudioValidationPort;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoicePreferencePort;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoiceRecognitionPort;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.application.result.ValidatedVoiceAudio;
import com.vitahealth.tata.intakeexecution.application.result.VoiceConfirmationResult;
import com.vitahealth.tata.intakeexecution.application.result.VoiceConfirmationResult.VoiceConfirmationStatus;
import com.vitahealth.tata.intakeexecution.application.result.VoiceRecognitionResult;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeByVoiceCommand;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.VoiceConfirmationLanguage;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.domain.services.VoiceConfirmationValidationService;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Recognizes speech outside a transaction and delegates the final locked confirmation. */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ConfirmIntakeByVoiceCommandHandler {

  private final VoiceRecognitionPort voiceRecognition;
  private final AuthenticatedIntakeConfirmationService authenticatedConfirmation;
  private final IntakeConfirmationAccessPort access;
  private final IntakeRepository repository;
  private final ConfirmIntakeCommandService confirmIntake;
  private final VoiceConfirmationValidationService validation;
  private final VoicePreferencePort voicePreference;
  private final VoiceAudioValidationPort audioValidation;

  /** Injects required provider, evidence, confirmation and preference capabilities. */
  public ConfirmIntakeByVoiceCommandHandler(
      VoiceRecognitionPort voiceRecognition,
      IntakeRepository repository,
      ConfirmIntakeCommandService confirmIntake,
      VoicePreferencePort voicePreference,
      VoiceAudioValidationPort audioValidation,
      AuthenticatedIntakeConfirmationService authenticatedConfirmation,
      IntakeConfirmationAccessPort access) {
    this.voiceRecognition = voiceRecognition;
    this.authenticatedConfirmation = authenticatedConfirmation;
    this.access = access;
    this.repository = repository;
    this.confirmIntake = confirmIntake;
    this.validation = new VoiceConfirmationValidationService();
    this.voicePreference = voicePreference;
    this.audioValidation = audioValidation;
  }

  /**
   * Recognition runs outside a database transaction; the confirmation service locks and commits the
   * intake.
   */
  public VoiceConfirmationResult handle(ConfirmIntakeByVoiceCommand command) {
    return handle(command, Optional.empty());
  }

  /** Carries a nonblank revocable reference across the nontransactional provider call. */
  public VoiceConfirmationResult handleAuthenticated(
      ConfirmIntakeByVoiceCommand command, String reference) {
    if (reference == null || reference.isBlank())
      throw new IllegalArgumentException("session reference is required");
    return handle(command, Optional.of(reference));
  }

  private VoiceConfirmationResult handle(
      ConfirmIntakeByVoiceCommand command, Optional<String> reference) {
    var intake =
        repository
            .findById(command.intakeId())
            .orElseThrow(
                () ->
                    new IntakeApplicationException(
                        IntakeApplicationException.Code.INTAKE_NOT_FOUND, "intake not found"));

    reference.ifPresent(value -> access.requireAuthorized(value, intake.olderAdultId()));

    if (intake.status() == IntakeStatus.CONFIRMED || intake.status() == IntakeStatus.LATE) {
      return new VoiceConfirmationResult(
          VoiceConfirmationStatus.ALREADY_CONFIRMED,
          null,
          1.0,
          IntakeResultFromAggregateAssembler.toResult(intake, true));
    }

    if (intake.status() == IntakeStatus.OMITTED) {
      throw new IntakeApplicationException(
          IntakeApplicationException.Code.INTAKE_NOT_CONFIRMABLE,
          "intake can no longer be confirmed");
    }

    if (!voicePreference.isVoiceConfirmationEnabled(intake.olderAdultId())) {
      throw new IntakeApplicationException(
          IntakeApplicationException.Code.VOICE_CONFIRMATION_DISABLED,
          "voice confirmation is turned off for this user");
    }

    final ValidatedVoiceAudio recording;
    final VoiceConfirmationLanguage language;
    try {
      recording = audioValidation.validate(command.audio(), command.contentType());
      language = new VoiceConfirmationLanguage(command.language());
    } catch (IllegalArgumentException invalidInput) {
      throw new IntakeApplicationException(
          IntakeApplicationException.Code.VALIDATION_ERROR, "voice input validation failed");
    }
    var recognition = voiceRecognition.recognize(recording, language);

    return switch (recognition.status()) {
      case UNAVAILABLE ->
          new VoiceConfirmationResult(
              VoiceConfirmationStatus.PROVIDER_UNAVAILABLE, null, 0.0, null);
      case UNRECOGNIZED ->
          new VoiceConfirmationResult(
              VoiceConfirmationStatus.NOT_RECOGNIZED,
              recognition.transcript(),
              recognition.confidence(),
              null);
      case RECOGNIZED ->
          confirmRecognized(command, intake.medication().name(), recognition, reference);
    };
  }

  private VoiceConfirmationResult confirmRecognized(
      ConfirmIntakeByVoiceCommand command,
      String medicationName,
      VoiceRecognitionResult recognition,
      Optional<String> reference) {
    if (!validation.isValidConfirmation(
        recognition.transcript(), recognition.confidence(), medicationName)) {
      return new VoiceConfirmationResult(
          VoiceConfirmationStatus.NOT_VALIDATED,
          recognition.transcript(),
          recognition.confidence(),
          null);
    }

    var confirmation = new ConfirmIntakeCommand(command.intakeId(), ConfirmationChannel.VOICE);
    var outcome =
        reference.isPresent()
            ? authenticatedConfirmation.handle(confirmation, reference.orElseThrow())
            : confirmIntake.handle(confirmation);
    var result =
        switch (outcome) {
          case Result.Success<IntakeResult, ApplicationError> success -> success.value();
          case Result.Failure<IntakeResult, ApplicationError> failure ->
              throw new IntakeApplicationException(
                  IntakeApplicationException.Code.valueOf(failure.error().code()),
                  failure.error().message());
        };
    return new VoiceConfirmationResult(
        result.alreadyConfirmed()
            ? VoiceConfirmationStatus.ALREADY_CONFIRMED
            : VoiceConfirmationStatus.CONFIRMED,
        recognition.transcript(),
        recognition.confidence(),
        result);
  }
}
