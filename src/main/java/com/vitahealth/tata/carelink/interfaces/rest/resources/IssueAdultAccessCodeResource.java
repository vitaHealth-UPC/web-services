package com.vitahealth.tata.carelink.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/** Caregiver who requests a sign-in code for the adult of the relationship. */
public record IssueAdultAccessCodeResource(@NotBlank String caregiverId) {}
