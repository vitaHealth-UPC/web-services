package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import com.vitahealth.tata.intakeexecution.application.commandservices.GenerateIntakesCommandService;
import com.vitahealth.tata.intakeexecution.application.internal.assemblers.IntakeResultFromAggregateAssembler;
import com.vitahealth.tata.intakeexecution.application.internal.outboundservices.IntakeScheduleLock;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.commands.GenerateIntakesCommand;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeRecurrence;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ScheduledIntakeSlot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.domain.services.IntakeScheduleGenerationPolicy;
import com.vitahealth.tata.intakeexecution.domain.services.IntakeScheduleReconciliationPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reconciles scheduled intake evidence within the caller transaction. */
@Service
@Transactional
public class GenerateIntakesCommandHandler implements GenerateIntakesCommandService {
  private static final int GENERATION_DAYS = 7;

  private final IntakeRepository repository;
  private final Clock clock;
  private final IntakeScheduleLock scheduleLock;

  /** Injects persistence and the application clock without installing a production fallback. */
  public GenerateIntakesCommandHandler(
      IntakeRepository repository, Clock clock, IntakeScheduleLock scheduleLock) {
    this.repository = repository;
    this.clock = clock;
    this.scheduleLock = scheduleLock;
  }

  /** Generates the current horizon while retaining definitive intake outcomes. */
  @Override
  public List<IntakeResult> handle(GenerateIntakesCommand command) {
    requireScheduleReference(command);
    scheduleLock.reserve(command.treatmentId().trim());

    var now = clock.instant();
    var existing = repository.findFutureByTreatmentIdForUpdate(command.treatmentId().trim(), now);
    if (!command.active()) {
      var changes =
          IntakeScheduleReconciliationPolicy.reconcile(
              command.treatmentId(), command.olderAdultId(), existing, List.of(), now, Instant.MAX);
      repository.deleteAll(changes.cancelled());
      return List.of();
    }

    if (command.scheduledTimes().isEmpty()) {
      throw new IllegalArgumentException("at least one scheduled time is required");
    }

    var snapshot =
        new MedicationSnapshot(command.medicationName(), command.dose(), command.instructions());
    var recurrence =
        IntakeRecurrence.fromSnapshot(
            command.recurrenceMode(),
            command.recurrenceZone(),
            command.recurrenceAnchor(),
            command.frequency());
    var horizonEnd =
        IntakeScheduleGenerationPolicy.horizonEnd(now, recurrence.zone(), GENERATION_DAYS);
    var occurrences =
        IntakeScheduleGenerationPolicy.occurrences(
            recurrence, command.scheduledTimes(), now, horizonEnd);
    var expected =
        occurrences.stream()
            .map(
                instant ->
                    new ScheduledIntakeSlot(
                        command.treatmentId(),
                        command.olderAdultId(),
                        command.medicationId(),
                        snapshot,
                        instant))
            .toList();
    var changes =
        IntakeScheduleReconciliationPolicy.reconcile(
            command.treatmentId(), command.olderAdultId(), existing, expected, now, horizonEnd);
    repository.deleteAll(changes.cancelled());
    return repository.saveAll(changes.pending()).stream()
        .map(IntakeResultFromAggregateAssembler::toResult)
        .toList();
  }

  private static void requireScheduleReference(GenerateIntakesCommand command) {
    if (command.treatmentId() == null || command.treatmentId().isBlank()) {
      throw new IllegalArgumentException("treatmentId is required");
    }
    if (command.medicationId() == null || command.medicationId().isBlank()) {
      throw new IllegalArgumentException("medicationId is required");
    }
    if (command.olderAdultId() == null || command.olderAdultId().isBlank()) {
      throw new IllegalArgumentException("olderAdultId is required");
    }
  }
}
