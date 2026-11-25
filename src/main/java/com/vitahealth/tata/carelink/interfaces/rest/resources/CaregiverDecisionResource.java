package com.vitahealth.tata.carelink.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

/** The adult's decision about another caregiver. */
public record CaregiverDecisionResource(@NotNull Boolean accepted) {}
