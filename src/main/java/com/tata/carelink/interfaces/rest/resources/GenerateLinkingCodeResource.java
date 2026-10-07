package com.tata.carelink.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record GenerateLinkingCodeResource(
        @NotBlank String caregiverId,
        @NotBlank String olderAdultId
) {}
