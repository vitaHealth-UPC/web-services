package com.tata.adherenceanalytics.application.ports;

import java.time.Instant;
import java.util.List;

/** Read-only outcome contract; analytics never updates treatment or intake state. */
public interface IntakeOutcomePort {
    enum Status { PENDING, CONFIRMED, LATE, OMITTED }
    record Outcome(String medicationId, Instant scheduledAt, Status status) {}
    List<Outcome> find(String olderAdultId, Instant from, Instant to);
}
