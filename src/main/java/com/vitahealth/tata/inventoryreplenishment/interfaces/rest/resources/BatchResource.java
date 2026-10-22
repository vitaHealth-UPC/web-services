package com.vitahealth.tata.inventoryreplenishment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Immutable recorded receipt evidence; an absent lot remains null rather than being invented. */
public record BatchResource(
    String id,
    int quantity,
    Instant registeredAt,
    @Schema(description = "Optional original lot reference") String lot) {}
