package com.tata.carelink.domain.model.valueobjects;

import static com.tata.shared.domain.validation.DomainText.requireText;

public record EmergencyContact(String name, String relationship, String phone) {
    public EmergencyContact {
        name = requireText(name, "emergency contact name");
        relationship = requireText(relationship, "emergency contact relationship");
        phone = requireText(phone, "emergency contact phone");
    }

}
