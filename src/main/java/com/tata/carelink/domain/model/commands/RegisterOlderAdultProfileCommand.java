package com.tata.carelink.domain.model.commands;

import java.time.LocalDate;

public record RegisterOlderAdultProfileCommand(
        String caregiverId,
        String fullName,
        LocalDate birthDate,
        String emergencyContactName,
        String emergencyContactRelationship,
        String emergencyContactPhone
) {}
