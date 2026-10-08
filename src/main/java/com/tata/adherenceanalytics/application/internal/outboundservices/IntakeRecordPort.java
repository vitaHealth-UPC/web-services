package com.tata.adherenceanalytics.application.internal.outboundservices;

import java.time.Instant;
import java.util.List;

/** Read-only view of intake results used by the adherence screens; analytics never changes intake state. */
public interface IntakeRecordPort {
    enum Status { PENDING, CONFIRMED, LATE, OMITTED }

    record IntakeRecord(String medicationName, Instant scheduledAt, Instant confirmedAt, Status status) {}

    /** Intakes of the older adult scheduled from the inclusive start to the exclusive end. */
    List<IntakeRecord> find(String olderAdultId, Instant from, Instant to);
}
