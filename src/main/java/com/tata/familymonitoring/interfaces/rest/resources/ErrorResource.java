package com.tata.familymonitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error returned by the API. The code is stable and meant for clients")
public record ErrorResource(
    @Schema(example = "NOT_FOUND") String code,
    @Schema(example = "Alert 99 was not found") String message) {
}
