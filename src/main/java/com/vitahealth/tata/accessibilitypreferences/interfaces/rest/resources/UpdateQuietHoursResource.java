package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Replaces one user's silence interval and explicit channel choices in a single request. */
@Schema(description = "Replaces the quiet hours and the notification channels of the user")
public record UpdateQuietHoursResource(
    @Schema(description = "Send null to remove the quiet hours", nullable = true) @Valid
        QuietHoursResource quietHours,
    @Schema(description = "Each channel type may appear only once") @NotNull @Size(max = 4)
        List<@NotNull @Valid ChannelResource> channels) {
  /** Copies submitted choices; absence remains available for explicit request validation. */
  public UpdateQuietHoursResource {
    if (channels != null) channels = List.copyOf(channels);
  }
}
