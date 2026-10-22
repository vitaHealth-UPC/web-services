package com.vitahealth.tata.identitysubscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/** Requests a catalog plan selection for the authenticated account. */
public record ChangeSubscriptionResource(@NotBlank String planCode) {}
