package com.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Replaces the quiet hours and the notification channels of the user")
public record UpdateQuietHoursResource(
        @Schema(description = "Send null to remove the quiet hours", nullable = true) @Valid QuietHoursResource quietHours,
        @Schema(description = "Each channel type may appear only once") @NotNull List<@NotNull @Valid ChannelResource> channels
) {}
