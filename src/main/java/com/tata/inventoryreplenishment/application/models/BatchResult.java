package com.tata.inventoryreplenishment.application.models;
import java.time.Instant;
public record BatchResult(String id, int quantity, Instant registeredAt, String lot) {
    public BatchResult(String id, int quantity, Instant registeredAt) { this(id, quantity, registeredAt, null); }
}
