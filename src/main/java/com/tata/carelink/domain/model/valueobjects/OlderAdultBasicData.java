package com.tata.carelink.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Objects;

public record OlderAdultBasicData(String fullName, LocalDate birthDate) {
    public OlderAdultBasicData {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName is required");
        }
        Objects.requireNonNull(birthDate, "birthDate is required");
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("birthDate cannot be in the future");
        }
        fullName = fullName.trim();
    }
}
