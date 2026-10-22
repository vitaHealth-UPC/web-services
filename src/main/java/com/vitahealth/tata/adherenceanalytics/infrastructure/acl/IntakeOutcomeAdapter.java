package com.vitahealth.tata.adherenceanalytics.infrastructure.acl;

import com.vitahealth.tata.adherenceanalytics.application.exceptions.AdherenceSourceUnavailableException;
import com.vitahealth.tata.adherenceanalytics.application.exceptions.InvalidAdherenceSourceException;
import com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices.IntakeOutcomePort;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.IntakeOutcome;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.IntakeOutcome.Status;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/** Translates public Intake evidence into the analytics-owned outcome model. */
@Component
public class IntakeOutcomeAdapter implements IntakeOutcomePort {
  private final IntakeContextFacade intakes;
  private final IntakeEvidenceReader evidence;

  /** Injects the public Intake boundary without depending on its persistence. */
  public IntakeOutcomeAdapter(IntakeContextFacade intakes, IntakeEvidenceReader evidence) {
    this.intakes = intakes;
    this.evidence = evidence;
  }

  /** Preserves source schedule and explicitly translates recorded outcome states. */
  @Override
  public List<IntakeOutcome> find(String olderAdultId, Instant from, Instant to) {
    return evidence.find(olderAdultId, from, to).stream()
        .map(
            intake ->
                new IntakeOutcome(
                    intake.medicationId(),
                    intake.scheduledAt(),
                    switch (intake.status()) {
                      case PENDING -> Status.PENDING;
                      case CONFIRMED -> Status.CONFIRMED;
                      case LATE -> Status.LATE;
                      case OMITTED -> Status.OMITTED;
                    }))
        .toList();
  }

  /** Discovers actual source owners for scheduled consolidation. */
  @Override
  public List<String> olderAdultIdsWithOutcomes(Instant from, Instant to, String after, int limit) {
    final List<String> source;
    try {
      source = intakes.findOlderAdultIdsWithIntakes(from, to, after, limit);
    } catch (RuntimeException failure) {
      throw new AdherenceSourceUnavailableException(failure);
    }
    try {
      var owners = List.copyOf(source);
      if (owners.size() > limit) throw new IllegalArgumentException("owner page exceeds limit");
      String cursor = after;
      for (var owner : owners) {
        if (owner.isBlank() || (cursor != null && owner.compareTo(cursor) <= 0)) {
          throw new IllegalArgumentException("source owner cursor did not advance");
        }
        cursor = owner;
      }
      return owners;
    } catch (IllegalArgumentException | NullPointerException failure) {
      throw new InvalidAdherenceSourceException(failure);
    }
  }
}
