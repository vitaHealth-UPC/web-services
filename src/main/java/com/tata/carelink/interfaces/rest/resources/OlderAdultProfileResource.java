package com.tata.carelink.interfaces.rest.resources;

import java.time.Instant;
import java.time.LocalDate;

public record OlderAdultProfileResource(
        String id,
        String registeredByCaregiverId,
        String fullName,
        LocalDate birthDate,
        String emergencyContactName,
        String emergencyContactRelationship,
        String emergencyContactPhone,
        Instant createdAt
) {}
