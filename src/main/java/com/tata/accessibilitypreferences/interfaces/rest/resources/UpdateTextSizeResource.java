package com.tata.accessibilitypreferences.interfaces.rest.resources;

import com.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change the text size")
public record UpdateTextSizeResource(
        @Schema(description = "SMALL, MEDIUM, LARGE or EXTRA_LARGE", example = "LARGE") @NotNull TextSizeLevel textSize
) {}
