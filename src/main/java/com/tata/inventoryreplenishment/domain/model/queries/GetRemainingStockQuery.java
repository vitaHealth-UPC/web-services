package com.tata.inventoryreplenishment.domain.model.queries;

public record GetRemainingStockQuery(String medicationId) {
    public GetRemainingStockQuery {
        if (medicationId == null || medicationId.isBlank()) {
            throw new IllegalArgumentException("medicationId is required");
        }
        medicationId = medicationId.trim();
    }
}
