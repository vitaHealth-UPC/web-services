package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import com.vitahealth.tata.intakeexecution.application.IntakeApplicationException;
import com.vitahealth.tata.intakeexecution.application.internal.assemblers.IntakeResultFromAggregateAssembler;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.VoicePreferencePort;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.events.IntakeConfirmedEventPublisher;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Confirms intake evidence and coordinates dependent effects atomically. */
@Service
@Transactional
public class ConfirmIntakeCommandHandler {
  private final IntakeRepository repository;
  private final IntakeConfirmedEventPublisher events;
  private final Clock clock;
  private final VoicePreferencePort voicePreference;

  /** Injects persistence, typed event delivery, application time and the live voice preference. */
  public ConfirmIntakeCommandHandler(
      IntakeRepository repository,
      IntakeConfirmedEventPublisher events,
      Clock clock,
      VoicePreferencePort voicePreference) {
    this.repository = repository;
    this.events = events;
    this.clock = clock;
    this.voicePreference = voicePreference;
  }

  /**
   * Locks evidence, revalidates voice permission and publishes a newly confirmed transition once.
   */
  public IntakeResult handle(ConfirmIntakeCommand command) {
    var intake =
        repository
            .findByIdForConfirmation(command.intakeId())
            .orElseThrow(
                () ->
                    new IntakeApplicationException(
                        IntakeApplicationException.Code.INTAKE_NOT_FOUND, "intake not found"));

    if (intake.isPending()
        && command.channel() == ConfirmationChannel.VOICE
        && !voicePreference.isVoiceConfirmationEnabled(intake.olderAdultId())) {
      throw new IntakeApplicationException(
          IntakeApplicationException.Code.VOICE_CONFIRMATION_DISABLED,
          "voice confirmation is turned off for this user");
    }

    var receivedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
    var occurredAt =
        command.confirmedAt() == null
            ? receivedAt
            : command.confirmedAt().truncatedTo(ChronoUnit.MICROS);
    if (command.confirmedAt() != null
        && (command.channel() != ConfirmationChannel.TOUCH
            || command.confirmedAt().isAfter(receivedAt)
            || occurredAt.isBefore(intake.createdAt())))
      throw new IntakeApplicationException(
          IntakeApplicationException.Code.VALIDATION_ERROR,
          "confirmation time must be between intake creation and server receipt");

    final boolean changed;
    try {
      changed = intake.confirm(occurredAt, command.channel(), command.confirmedAt() != null);
    } catch (IllegalStateException exception) {
      throw new IntakeApplicationException(
          IntakeApplicationException.Code.INTAKE_NOT_CONFIRMABLE, exception.getMessage());
    }

    if (changed) {
      repository.saveAll(List.of(intake));
      events.publish(
          new IntakeConfirmed(
              intake.id(), intake.medicationId(), intake.olderAdultId(), intake.confirmedAt()));
    }

    return IntakeResultFromAggregateAssembler.toResult(intake, !changed);
  }
}
