package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Public preference snapshot with isolated channel and contact lists. */
@Schema(description = "Accessibility and notification preferences of a user")
public record UserPreferencesResource(
    @Schema(example = "00000000-0000-0000-0000-000000000001") String userId,
    @Schema(example = "MEDIUM") TextSizeLevel textSize,
    @Schema(example = "false") boolean highContrast,
    @Schema(example = "false") boolean reducedMotion,
    @Schema(example = "false") boolean readingAssistance,
    @Schema(example = "true") boolean voiceConfirmationEnabled,
    @Schema(description = "null when the user has no quiet hours", nullable = true)
        QuietHoursResource quietHours,
    List<ChannelResource> notificationChannels,
    NotificationPolicyResource notificationPolicy,
    List<EmergencyContactResource> emergencyContacts,
    boolean darkTheme) {
  /** Retains immutable response collections independently of the caller's list instances. */
  public UserPreferencesResource {
    notificationChannels = List.copyOf(notificationChannels);
    emergencyContacts = List.copyOf(emergencyContacts);
  }
}
