package com.tata.inventoryreplenishment.interfaces.rest.resources;
import java.time.Instant;
public record BatchResource(String id, int quantity, Instant registeredAt, String lot) {
    public BatchResource(String id, int quantity, Instant registeredAt) { this(id, quantity, registeredAt, null); }
}
