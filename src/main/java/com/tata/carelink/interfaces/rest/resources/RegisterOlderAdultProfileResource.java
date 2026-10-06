package com.tata.carelink.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RegisterOlderAdultProfileResource(
        @NotBlank String caregiverId,
        @NotBlank String fullName,
        @NotNull LocalDate birthDate,
        String emergencyContactName,
        String emergencyContactRelationship,
        String emergencyContactPhone
) {}
