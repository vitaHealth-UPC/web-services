package com.vitahealth.tata.carelink.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

/** Carries the adult decision for consent onboarding. */
public record RegisterConsentResource(@NotNull Boolean accepted) {}
