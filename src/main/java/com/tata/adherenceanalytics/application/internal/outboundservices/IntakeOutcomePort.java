package com.tata.adherenceanalytics.application.internal.outboundservices;

import java.time.Instant;
import com.tata.adherenceanalytics.application.models.IntakeOutcome;
import java.util.List;

/** Read-only outcome contract; analytics never updates treatment or intake state. */
public interface IntakeOutcomePort {
    List<IntakeOutcome> find(String olderAdultId, Instant from, Instant to);
    /** Older adults that have intakes scheduled in the period; the weekly consolidation covers them. */
    default List<String> olderAdultIdsWithOutcomes(Instant from, Instant to) { return List.of(); }
}
