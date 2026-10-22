package com.vitahealth.tata.familymonitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes a stable client error code and a localized message without internal exception details.
 */
@Schema(description = "Error returned by the API. The code is stable and meant for clients")
public record ErrorResource(
    @Schema(example = "NOT_FOUND") String code,
    @Schema(example = "The requested follow-up information was not found") String message) {}
