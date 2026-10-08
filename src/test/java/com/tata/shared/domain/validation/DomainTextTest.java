package com.tata.shared.domain.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainTextTest {
    @Test
    void normalizesRequiredTextWithoutChangingInteriorSpaces() {
        assertEquals("Rosa Vargas", DomainText.requireText(" Rosa Vargas ", "name"));
        assertEquals("1  comprimido", DomainText.requireText(" 1  comprimido ", "dose"));
    }

    @Test
    void rejectsMissingValuesWithTheExistingFieldMessage() {
        for (String value : new String[] {null, "", " \t\n", "\u2003"}) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> DomainText.requireText(value, "name"));
            assertEquals("name is required", error.getMessage());
        }
    }
}
