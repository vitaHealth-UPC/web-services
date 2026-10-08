package com.tata.accessibilitypreferences.interfaces.rest.resources;

import com.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Notification channel and whether the user allows it")
public record ChannelResource(
        @Schema(example = "PUSH") @NotNull ChannelType type,
        @Schema(example = "true") boolean enabled
) {}
