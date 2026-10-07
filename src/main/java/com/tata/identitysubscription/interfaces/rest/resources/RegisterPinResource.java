package com.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterPinResource(
        @NotBlank String olderAdultId,
        @NotBlank @Pattern(regexp = "\\d{4}") String pin
) {}
