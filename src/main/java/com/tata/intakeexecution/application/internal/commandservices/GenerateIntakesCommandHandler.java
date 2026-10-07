package com.tata.intakeexecution.application.internal.commandservices;

import com.tata.intakeexecution.domain.model.commands.GenerateIntakesCommand;
import com.tata.intakeexecution.application.internal.IntakeMapper;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class GenerateIntakesCommandHandler {
    private static final int GENERATION_DAYS = 7;

    private final IntakeRepository repository;
    private final Clock clock;

    @Autowired
    public GenerateIntakesCommandHandler(IntakeRepository repository) {
        this(repository, Clock.systemUTC());
    }

    GenerateIntakesCommandHandler(IntakeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<IntakeResult> handle(GenerateIntakesCommand command) {
        requireScheduleReference(command);

        var now = clock.instant();
        var existing = repository.findFutureByTreatmentId(command.treatmentId().trim(), now);
        var pending = existing.stream().filter(Intake::isPending).toList();
        var definitiveTimes = new HashSet<>(
                existing.stream().filter(intake -> !intake.isPending()).map(Intake::scheduledAt).toList()
        );

        repository.deleteAll(pending);

        if (!command.active()) {
            return List.of();
        }

        if (command.scheduledTimes().isEmpty()) {
            throw new IllegalArgumentException("at least one scheduled time is required");
        }

        var snapshot = new MedicationSnapshot(
                command.medicationName(),
                command.dose(),
                command.instructions()
        );
        var startDate = LocalDate.ofInstant(now, ZoneOffset.UTC);
        var generated = new ArrayList<Intake>();

        for (int day = 0; day < GENERATION_DAYS; day++) {
            var date = startDate.plusDays(day);
            command.scheduledTimes().stream().distinct().sorted().forEach(time -> {
                var scheduledAt = date.atTime(time).toInstant(ZoneOffset.UTC);
                if (!scheduledAt.isBefore(now) && !definitiveTimes.contains(scheduledAt)) {
                    generated.add(Intake.createScheduled(
                            command.treatmentId().trim(),
                            command.medicationId().trim(),
                            command.olderAdultId().trim(),
                            snapshot,
                            scheduledAt,
                            now
                    ));
                }
            });
        }

        return repository.saveAll(generated).stream()
                .map(IntakeMapper::toResult)
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
