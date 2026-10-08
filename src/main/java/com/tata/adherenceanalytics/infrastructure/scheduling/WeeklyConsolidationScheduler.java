package com.tata.adherenceanalytics.infrastructure.scheduling;

import com.tata.adherenceanalytics.application.internal.commandservices.ConsolidateWeeklyPeriodCommandHandler;
import com.tata.adherenceanalytics.application.internal.outboundservices.IntakeOutcomePort;
import java.time.Instant;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consolidates the last seven full days of every older adult that had intakes. The period is cut at
 * midnight of the calendar zone, so running it again the same day finds the stored evidence and
 * does not repeat anything.
 */
@Component
public class WeeklyConsolidationScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeeklyConsolidationScheduler.class);

    private final ConsolidateWeeklyPeriodCommandHandler consolidations;
    private final IntakeOutcomePort outcomes;
    private final String zone;

    public WeeklyConsolidationScheduler(
            ConsolidateWeeklyPeriodCommandHandler consolidations,
            IntakeOutcomePort outcomes,
            @Value("${adherence.consolidation.zone:America/Lima}") String zone) {
        this.consolidations = consolidations;
        this.outcomes = outcomes;
        this.zone = zone;
    }

    @Scheduled(
            cron = "${adherence.consolidation.cron:0 0 3 * * *}",
            zone = "${adherence.consolidation.zone:America/Lima}")
    public void consolidateLastWeek() {
        consolidateWeekEnding(Instant.now());
    }

    /** Consolidates the seven days that ended at the last midnight before {@code now}. */
    public void consolidateWeekEnding(Instant now) {
        var calendarZone = ZoneId.of(zone);
        var to = now.atZone(calendarZone).toLocalDate().atStartOfDay(calendarZone).toInstant();
        var from = now.atZone(calendarZone).toLocalDate().minusDays(7).atStartOfDay(calendarZone).toInstant();
        for (var olderAdultId : outcomes.olderAdultIdsWithOutcomes(from, to)) {
            try {
                consolidations.handle(olderAdultId, from, to, zone);
            } catch (RuntimeException exception) {
                LOGGER.error("Weekly adherence of {} could not be consolidated", olderAdultId, exception);
            }
        }
    }
}
