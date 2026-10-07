package com.tata.familymonitoring.application.internal.outboundservices.acl;

import com.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import com.tata.familymonitoring.domain.ports.IIntakeHistoryPort;
import java.time.Instant;
import com.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Maps Intake Execution query results into the Family Monitoring contract. */
@Component
public class IntakeHistoryAdapter implements IIntakeHistoryPort {
    private final IntakeContextFacade intakes;
    public IntakeHistoryAdapter(IntakeContextFacade intakes) { this.intakes=intakes; }
    @Override public List<IntakeSummary> getRecentIntakes(String owner, int days) {
        var to = Instant.now();
        return intakes.history(owner, to.minus(Duration.ofDays(days)), to).stream()
                .map(intake -> new IntakeSummary(intake.id(), intake.medicationName(), intake.scheduledAt(),
                        IntakeStatus.valueOf(intake.status().name()))).toList();
    }
    @Override public Optional<Instant> findNextIntakeAt(String owner) {
        return intakes.next(owner).map(intake -> intake.scheduledAt());
    }
}
