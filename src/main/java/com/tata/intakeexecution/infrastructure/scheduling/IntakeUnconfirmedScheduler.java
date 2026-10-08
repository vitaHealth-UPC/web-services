package com.tata.intakeexecution.infrastructure.scheduling;

import com.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakesCommand;
import com.tata.intakeexecution.application.internal.commandservices.ReportUnconfirmedIntakesCommandHandler;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IntakeUnconfirmedScheduler {

    private final ReportUnconfirmedIntakesCommandHandler handler;
    private final Duration unconfirmedAfter;
    private final Clock clock;

    @Autowired
    public IntakeUnconfirmedScheduler(
            ReportUnconfirmedIntakesCommandHandler handler,
            @Value("${tata.intake.unconfirmed-after:PT15M}") Duration unconfirmedAfter
    ) {
        this(handler, unconfirmedAfter, Clock.systemUTC());
    }

    IntakeUnconfirmedScheduler(
            ReportUnconfirmedIntakesCommandHandler handler,
            Duration unconfirmedAfter,
            Clock clock
    ) {
        if (unconfirmedAfter.isNegative()) {
            throw new IllegalArgumentException("unconfirmedAfter cannot be negative");
        }
        this.handler = handler;
        this.unconfirmedAfter = unconfirmedAfter;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${tata.intake.unconfirmed-evaluation-interval:PT1M}",
            initialDelayString = "${tata.intake.unconfirmed-evaluation-initial-delay:PT1M}"
    )
    public void reportUnconfirmedIntakes() {
        var now = clock.instant();
        handler.handle(new ReportUnconfirmedIntakesCommand(now.minus(unconfirmedAfter), now));
    }
}
