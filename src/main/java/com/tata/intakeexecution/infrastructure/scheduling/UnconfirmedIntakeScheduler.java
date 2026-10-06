package com.tata.intakeexecution.infrastructure.scheduling;

import com.tata.intakeexecution.application.internal.commandservices.ReportUnconfirmedIntakeCommandHandler;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class UnconfirmedIntakeScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnconfirmedIntakeScheduler.class);
    private final IntakeRepository repository;
    private final ReportUnconfirmedIntakeCommandHandler report;
    public UnconfirmedIntakeScheduler(IntakeRepository repository, ReportUnconfirmedIntakeCommandHandler report) {
        this.repository = repository; this.report = report;
    }
    @Scheduled(fixedDelayString = "${tata.intake.unconfirmed-interval:PT1M}", initialDelayString = "${tata.intake.unconfirmed-initial-delay:PT1M}")
    public void evaluateDueIntakes() { evaluate(Instant.now()); }
    public void evaluate(Instant now) {
        for (var id : repository.findUnreportedDueIds(now)) {
            try { report.handle(id, now); }
            catch (RuntimeException exception) { LOGGER.error("Could not report unconfirmed intake {} ({})", id, exception.getClass().getSimpleName()); }
        }
    }
}
