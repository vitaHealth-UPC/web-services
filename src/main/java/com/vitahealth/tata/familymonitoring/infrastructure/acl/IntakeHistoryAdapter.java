package com.vitahealth.tata.familymonitoring.infrastructure.acl;

import com.vitahealth.tata.familymonitoring.application.exceptions.InvalidMonitoringSourceException;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.IntakeSummary;
import com.vitahealth.tata.familymonitoring.domain.ports.IntakeHistoryPort;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Maps Intake Execution query results into the Family Monitoring contract. */
@Component
public class IntakeHistoryAdapter implements IntakeHistoryPort {
  private final IntakeContextFacade intakes;
  private final Clock clock;

  /** Injects intake queries and the application clock for rolling history windows. */
  public IntakeHistoryAdapter(IntakeContextFacade intakes, Clock clock) {
    this.intakes = intakes;
    this.clock = clock;
  }

  /** Maps history over the requested number of elapsed days ending at the application clock. */
  @Override
  public List<IntakeSummary> getRecentIntakes(String owner, int days) {
    var to = clock.instant();
    try {
      return intakes.history(owner, to.minus(Duration.ofDays(days)), to).stream()
          .map(
              intake -> {
                if (!owner.equals(intake.olderAdultId()))
                  throw new IllegalStateException(
                      "source intake owner differs from requested adult");
                return new IntakeSummary(
                    intake.id(),
                    intake.medicationName(),
                    intake.scheduledAt(),
                    IntakeStatus.valueOf(intake.status().name()));
              })
          .toList();
    } catch (IllegalArgumentException
        | IllegalStateException
        | NullPointerException invalidSource) {
      throw new InvalidMonitoringSourceException(invalidSource);
    }
  }

  /** Returns the next intake selected by the source context. */
  @Override
  public Optional<Instant> findNextIntakeAt(String owner) {
    try {
      return intakes
          .next(owner)
          .map(
              intake -> {
                if (!owner.equals(intake.olderAdultId()))
                  throw new IllegalStateException(
                      "source next-intake owner differs from requested adult");
                return Objects.requireNonNull(intake.scheduledAt(), "source next-intake schedule");
              });
    } catch (IllegalArgumentException
        | IllegalStateException
        | NullPointerException invalidSource) {
      throw new InvalidMonitoringSourceException(invalidSource);
    }
  }
}
