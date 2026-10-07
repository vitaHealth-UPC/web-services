package com.tata.intakeexecution.application.internal.commandservices;

import com.tata.intakeexecution.application.commands.ConfirmIntakeByVoiceCommand;
import com.tata.intakeexecution.application.commands.ConfirmIntakeCommand;
import com.tata.intakeexecution.application.internal.IntakeApplicationException;
import com.tata.intakeexecution.application.internal.IntakeMapper;
import com.tata.intakeexecution.application.internal.outboundservices.IVoicePreferencePort;
import com.tata.intakeexecution.application.internal.outboundservices.IVoiceRecognitionPort;
import com.tata.intakeexecution.application.models.VoiceConfirmationResult;
import com.tata.intakeexecution.application.models.VoiceConfirmationResult.VoiceConfirmationStatus;
import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.tata.intakeexecution.domain.services.VoiceConfirmationValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmIntakeByVoiceCommandHandler {

    private final IVoiceRecognitionPort voiceRecognition;
    private final IntakeRepository repository;
    private final ConfirmIntakeCommandHandler confirmIntake;
    private final VoiceConfirmationValidationService validation;
    private final IVoicePreferencePort voicePreference;

    @Autowired
    public ConfirmIntakeByVoiceCommandHandler(
            IVoiceRecognitionPort voiceRecognition,
            IntakeRepository repository,
            ConfirmIntakeCommandHandler confirmIntake,
            IVoicePreferencePort voicePreference
    ) {
        this(
                voiceRecognition,
                repository,
                confirmIntake,
                new VoiceConfirmationValidationService(),
                voicePreference
        );
    }

    /** Voice is allowed for everybody; used where no preference source is needed. */
    public ConfirmIntakeByVoiceCommandHandler(
            IVoiceRecognitionPort voiceRecognition,
            IntakeRepository repository,
            ConfirmIntakeCommandHandler confirmIntake
    ) {
        this(voiceRecognition, repository, confirmIntake, olderAdultId -> true);
    }

    ConfirmIntakeByVoiceCommandHandler(
            IVoiceRecognitionPort voiceRecognition,
            IntakeRepository repository,
            ConfirmIntakeCommandHandler confirmIntake,
            VoiceConfirmationValidationService validation
    ) {
        this(voiceRecognition, repository, confirmIntake, validation, olderAdultId -> true);
    }

    ConfirmIntakeByVoiceCommandHandler(
            IVoiceRecognitionPort voiceRecognition,
            IntakeRepository repository,
            ConfirmIntakeCommandHandler confirmIntake,
            VoiceConfirmationValidationService validation,
            IVoicePreferencePort voicePreference
    ) {
        this.voiceRecognition = voiceRecognition;
        this.repository = repository;
        this.confirmIntake = confirmIntake;
        this.validation = validation;
        this.voicePreference = voicePreference;
    }

    @Transactional
    public VoiceConfirmationResult handle(ConfirmIntakeByVoiceCommand command) {
        var intake = repository.findById(command.intakeId())
                .orElseThrow(() -> new IntakeApplicationException(
                        IntakeApplicationException.Code.INTAKE_NOT_FOUND,
                        "intake not found"
                ));

        if (intake.status() == IntakeStatus.CONFIRMED || intake.status() == IntakeStatus.LATE) {
            return new VoiceConfirmationResult(
                    VoiceConfirmationStatus.ALREADY_CONFIRMED,
                    null,
                    1.0,
                    IntakeMapper.toResult(intake)
            );
        }

        if (intake.status() == IntakeStatus.OMITTED) {
            throw new IntakeApplicationException(
                    IntakeApplicationException.Code.INTAKE_NOT_CONFIRMABLE,
                    "intake can no longer be confirmed"
            );
        }

        if (!voicePreference.isVoiceConfirmationEnabled(intake.olderAdultId())) {
            throw new IntakeApplicationException(
                    IntakeApplicationException.Code.VOICE_CONFIRMATION_DISABLED,
                    "voice confirmation is turned off for this user"
            );
        }

        var recognition = voiceRecognition.recognize(
                command.audio(),
                command.contentType(),
                command.language()
        );

        return switch (recognition.status()) {
            case UNAVAILABLE -> new VoiceConfirmationResult(
                    VoiceConfirmationStatus.PROVIDER_UNAVAILABLE,
                    null,
                    0.0,
                    null
            );
            case UNRECOGNIZED -> new VoiceConfirmationResult(
                    VoiceConfirmationStatus.NOT_RECOGNIZED,
                    recognition.transcript(),
                    recognition.confidence(),
                    null
            );
            case RECOGNIZED -> confirmRecognized(command, intake.medication().name(), recognition);
        };
    }

    private VoiceConfirmationResult confirmRecognized(
            ConfirmIntakeByVoiceCommand command,
            String medicationName,
            IVoiceRecognitionPort.VoiceRecognitionResult recognition
    ) {
        if (!validation.isValidConfirmation(
                recognition.transcript(),
                recognition.confidence(),
                medicationName)) {
            return new VoiceConfirmationResult(
                    VoiceConfirmationStatus.NOT_VALIDATED,
                    recognition.transcript(),
                    recognition.confidence(),
                    null
            );
        }

        var result = confirmIntake.handle(
                new ConfirmIntakeCommand(command.intakeId(), ConfirmationChannel.VOICE)
        );
        return new VoiceConfirmationResult(
                VoiceConfirmationStatus.CONFIRMED,
                recognition.transcript(),
                recognition.confidence(),
                result
        );
    }
}
