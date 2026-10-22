package com.vitahealth.tata.adherenceanalytics.infrastructure.acl;

import com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices.IntakeRecordPort;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceIntakeRecord;
import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceIntakeRecord.Status;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/** Translates validated Intake evidence into the analytics screen projection. */
@Component
public class IntakeRecordAdapter implements IntakeRecordPort {
  private final IntakeEvidenceReader evidence;

  /** Injects the common source evidence validation boundary. */
  public IntakeRecordAdapter(IntakeEvidenceReader evidence) {
    this.evidence = evidence;
  }

  /** Preserves schedule and confirmation evidence with explicit state translation. */
  @Override
  public List<AdherenceIntakeRecord> find(String olderAdultId, Instant from, Instant to) {
    return evidence.find(olderAdultId, from, to).stream()
        .map(
            intake ->
                new AdherenceIntakeRecord(
                    intake.medicationName(),
                    intake.scheduledAt(),
                    intake.confirmedAt(),
                    switch (intake.status()) {
                      case PENDING -> Status.PENDING;
                      case CONFIRMED -> Status.CONFIRMED;
                      case LATE -> Status.LATE;
                      case OMITTED -> Status.OMITTED;
                    }))
        .toList();
  }
}
