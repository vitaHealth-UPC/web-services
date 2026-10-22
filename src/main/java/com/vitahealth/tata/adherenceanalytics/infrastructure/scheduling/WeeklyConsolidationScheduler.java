package com.vitahealth.tata.adherenceanalytics.infrastructure.scheduling;

import com.vitahealth.tata.adherenceanalytics.application.commandservices.AdherenceConsolidationCommandService;
import com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices.IntakeOutcomePort;
import com.vitahealth.tata.adherenceanalytics.domain.model.commands.ConsolidateWeeklyPeriodCommand;
import java.time.Clock;
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

  private final AdherenceConsolidationCommandService consolidations;
  private final IntakeOutcomePort outcomes;
  private final String zone;
  private final Clock clock;

  /** Validates the calendar zone and injects application contracts and shared time. */
  public WeeklyConsolidationScheduler(
      AdherenceConsolidationCommandService consolidations,
      IntakeOutcomePort outcomes,
      @Value("${adherence.consolidation.zone:America/Lima}") String zone,
      Clock clock) {
    this.consolidations = consolidations;
    this.outcomes = outcomes;
    this.zone = ZoneId.of(zone).getId();
    this.clock = clock;
  }

  /** Runs the bounded participant scan using the shared reference clock. */
  @Scheduled(
      cron = "${adherence.consolidation.cron:0 0 3 * * *}",
      zone = "${adherence.consolidation.zone:America/Lima}")
  public void consolidateLastWeek() {
    consolidateWeekEnding(clock.instant());
  }

  /** Consolidates the seven days that ended at the last midnight before {@code now}. */
  public void consolidateWeekEnding(Instant now) {
    var calendarZone = ZoneId.of(zone);
    var to = now.atZone(calendarZone).toLocalDate().atStartOfDay(calendarZone).toInstant();
    var from =
        now.atZone(calendarZone).toLocalDate().minusDays(7).atStartOfDay(calendarZone).toInstant();
    String cursor = null;
    while (true) {
      var owners = outcomes.olderAdultIdsWithOutcomes(from, to, cursor, 100);
      if (owners.isEmpty()) return;
      for (var olderAdultId : owners) {
        if (olderAdultId == null
            || olderAdultId.isBlank()
            || (cursor != null && olderAdultId.compareTo(cursor) <= 0)) {
          throw new IllegalStateException("source owner cursor did not advance");
        }
        cursor = olderAdultId;
        try {
          consolidations.handle(new ConsolidateWeeklyPeriodCommand(olderAdultId, from, to, zone));
        } catch (RuntimeException exception) {
          LOGGER.error("Weekly adherence of {} could not be consolidated", olderAdultId, exception);
        }
      }
    }
  }
}
