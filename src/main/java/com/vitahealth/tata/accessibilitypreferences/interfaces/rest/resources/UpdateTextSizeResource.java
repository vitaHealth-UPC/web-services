package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Requires one supported text-size choice for a complete replacement request. */
@Schema(description = "Request to change the text size")
public record UpdateTextSizeResource(
    @Schema(description = "SMALL, MEDIUM, LARGE or EXTRA_LARGE", example = "LARGE") @NotNull
        TextSizeLevel textSize) {}
