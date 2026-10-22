package com.vitahealth.tata.intakeexecution.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Manual confirmation request; speech confirmation requires recognition evidence. */
public record ConfirmIntakeResource(
    @NotBlank
        @Pattern(regexp = "TOUCH")
        @Schema(
            description = "Manual confirmation channel",
            allowableValues = {"TOUCH"})
        String channel) {}
