package com.tata.carelink.domain.model.valueobjects;

public record EmergencyContact(String name, String relationship, String phone) {
    public EmergencyContact {
        name = requireText(name, "emergency contact name");
        relationship = requireText(relationship, "emergency contact relationship");
        phone = requireText(phone, "emergency contact phone");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
