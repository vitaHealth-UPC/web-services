package com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceIntakeRecord;
import java.time.Instant;
import java.util.List;

/**
 * Read-only view of intake results used by the adherence screens; analytics never changes intake
 * state.
 */
public interface IntakeRecordPort {
  /** Intakes of the older adult scheduled from the inclusive start to the exclusive end. */
  List<AdherenceIntakeRecord> find(String olderAdultId, Instant from, Instant to);
}
