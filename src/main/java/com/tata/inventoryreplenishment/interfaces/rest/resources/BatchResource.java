package com.tata.inventoryreplenishment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "A quantity of units registered at a given moment")
public record BatchResource(
        String id,
        @Schema(example = "30") int quantity,
        Instant registeredAt
) {}
