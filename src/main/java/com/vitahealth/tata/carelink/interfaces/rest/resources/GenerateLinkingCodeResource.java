package com.vitahealth.tata.carelink.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/** Requests issuance for the authenticated caregiver profile. */
public record GenerateLinkingCodeResource(
    @NotBlank String caregiverId, @NotBlank String olderAdultId) {}
