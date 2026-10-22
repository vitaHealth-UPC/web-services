package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Represents an explicit channel choice for replacement requests and saved preference responses.
 */
@Schema(description = "Notification channel and whether the user allows it")
public record ChannelResource(
    @Schema(example = "PUSH") @NotNull ChannelType type,
    @Schema(example = "true") @NotNull Boolean enabled) {}
