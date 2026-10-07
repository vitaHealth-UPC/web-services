package com.tata.familymonitoring.infrastructure.modules;

import com.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import com.tata.familymonitoring.domain.ports.IIntakeHistoryPort;
import com.tata.intakeexecution.application.internal.queryservices.GetIntakeHistoryQueryHandler;
import com.tata.intakeexecution.application.internal.queryservices.GetNextIntakeQueryHandler;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Maps Intake Execution query results into the Family Monitoring contract. */
@Component
public class IntakeHistoryAdapter implements IIntakeHistoryPort {
    private final GetIntakeHistoryQueryHandler history;
    private final GetNextIntakeQueryHandler next;
    public IntakeHistoryAdapter(GetIntakeHistoryQueryHandler history, GetNextIntakeQueryHandler next) {
        this.history = history;
        this.next = next;
    }
    @Override public List<IntakeSummary> getRecentIntakes(String owner, int days) {
        var to = Instant.now();
        return history.handle(owner, to.minus(Duration.ofDays(days)), to).stream()
                .map(intake -> new IntakeSummary(intake.id(), intake.medicationName(), intake.scheduledAt(),
                        IntakeStatus.valueOf(intake.status().name()))).toList();
    }
    @Override public Optional<Instant> findNextIntakeAt(String owner) {
        return next.handle(owner).map(intake -> intake.scheduledAt());
    }
}
