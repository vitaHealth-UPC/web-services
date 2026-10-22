package com.vitahealth.tata.adherenceanalytics.infrastructure.acl;

import com.vitahealth.tata.adherenceanalytics.application.exceptions.AdherenceSourceUnavailableException;
import com.vitahealth.tata.adherenceanalytics.application.exceptions.InvalidAdherenceSourceException;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Validates public Intake evidence before either analytics projection consumes it. */
@Component
public class IntakeEvidenceReader {
  private final IntakeContextFacade intakes;

  /** Injects the owning context's public read boundary. */
  public IntakeEvidenceReader(IntakeContextFacade intakes) {
    this.intakes = intakes;
  }

  /** Rejects inconsistent evidence instead of silently dropping or misattributing it. */
  public List<IntakeResult> find(String olderAdultId, Instant from, Instant to) {
    final List<IntakeResult> evidence;
    try {
      evidence = intakes.findEvidence(olderAdultId, from, to);
    } catch (RuntimeException failure) {
      throw new AdherenceSourceUnavailableException(failure);
    }
    try {
      var records = List.copyOf(evidence);
      var identities = new HashSet<String>();
      for (var record : records) {
        requireText(record.id());
        requireText(record.medicationId());
        requireText(record.medicationName());
        Objects.requireNonNull(record.status(), "intake status");
        Objects.requireNonNull(record.scheduledAt(), "intake schedule");
        boolean confirmed =
            switch (record.status()) {
              case CONFIRMED, LATE -> true;
              case PENDING, OMITTED -> false;
            };
        if ((confirmed && (record.confirmedAt() == null || record.confirmationChannel() == null))
            || (!confirmed
                && (record.confirmedAt() != null || record.confirmationChannel() != null))) {
          throw new IllegalArgumentException("intake confirmation evidence is inconsistent");
        }
        if (!olderAdultId.equals(record.olderAdultId())
            || record.scheduledAt().isBefore(from)
            || !record.scheduledAt().isBefore(to)
            || !identities.add(record.id())) {
          throw new IllegalArgumentException("intake owner, period or identity is inconsistent");
        }
      }
      return records;
    } catch (IllegalArgumentException | NullPointerException failure) {
      throw new InvalidAdherenceSourceException(failure);
    }
  }

  private static void requireText(String value) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("intake evidence text is required");
  }
}
