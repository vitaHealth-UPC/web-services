package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries a recipient name and submitted phone; domain conversion validates and normalizes the
 * phone.
 */
public record EmergencyContactResource(
    @NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 30) String phone) {}
