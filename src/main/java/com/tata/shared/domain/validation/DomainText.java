package com.tata.shared.domain.validation;

/** Text normalization shared by domain models; business rules stay in their bounded contexts. */
public final class DomainText {
    private DomainText() {}

    /**
     * Normalizes required text without changing internal whitespace.
     * @param value text to validate
     * @param field name used in the validation error
     * @return trimmed nonblank text
     * @throws IllegalArgumentException when the value is null or blank
     */
    public static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
