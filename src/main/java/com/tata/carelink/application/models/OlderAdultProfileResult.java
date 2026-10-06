package com.tata.carelink.application.models;

import java.time.Instant;
import java.time.LocalDate;

public record OlderAdultProfileResult(
        String id,
        String registeredByCaregiverId,
        String fullName,
        LocalDate birthDate,
        String emergencyContactName,
        String emergencyContactRelationship,
        String emergencyContactPhone,
        Instant createdAt
) {}
