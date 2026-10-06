package com.tata.inventoryreplenishment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error returned by the API. The code is stable and meant for clients; the message is localized")
public record ErrorResource(
        @Schema(example = "INVALID_QUANTITY") String code,
        @Schema(example = "Quantities must be positive and the replenishment threshold cannot be negative") String message
) {}
