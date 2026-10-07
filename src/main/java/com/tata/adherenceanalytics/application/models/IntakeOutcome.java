package com.tata.adherenceanalytics.application.models;
import java.time.Instant;
public record IntakeOutcome(String medicationId, Instant scheduledAt, Status status) {
    public enum Status { PENDING, CONFIRMED, LATE, OMITTED }
}
