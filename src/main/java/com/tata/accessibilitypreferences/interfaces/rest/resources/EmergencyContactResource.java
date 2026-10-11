package com.tata.accessibilitypreferences.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmergencyContactResource(
    @NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 30) String phone) {}
