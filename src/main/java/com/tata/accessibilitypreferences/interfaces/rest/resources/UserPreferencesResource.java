package com.tata.accessibilitypreferences.interfaces.rest.resources;

import com.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Accessibility and notification preferences of a user")
public record UserPreferencesResource(
        @Schema(example = "00000000-0000-0000-0000-000000000001") String userId,
        @Schema(example = "MEDIUM") TextSizeLevel textSize,
        @Schema(example = "false") boolean highContrast,
        @Schema(example = "false") boolean reducedMotion,
        @Schema(example = "false") boolean readingAssistance,
        @Schema(example = "true") boolean voiceConfirmationEnabled,
        @Schema(description = "null when the user has no quiet hours", nullable = true) QuietHoursResource quietHours,
        List<ChannelResource> notificationChannels
) {}
