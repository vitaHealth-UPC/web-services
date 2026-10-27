package com.vitahealth.tata.intakeexecution.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

/** Manual confirmation request; speech confirmation requires recognition evidence. */
public record ConfirmIntakeResource(
    @NotBlank
        @Pattern(regexp = "TOUCH")
        @Schema(
            description = "Manual confirmation channel",
            allowableValues = {"TOUCH"})
        String channel,
    @Schema(
            description =
                "Optional device-reported UTC action time for offline TOUCH replay; "
                    + "must be between slot creation and server receipt. Omit for server time.")
        Instant confirmedAt) {
  /** Preserves the manual request constructor used by existing clients and tests. */
  public ConfirmIntakeResource(String channel) {
    this(channel, null);
  }
}
