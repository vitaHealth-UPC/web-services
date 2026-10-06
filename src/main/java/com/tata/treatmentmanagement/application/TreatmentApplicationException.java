package com.tata.treatmentmanagement.application;

public final class TreatmentApplicationException extends RuntimeException {
    public enum Code {
        MEDICATION_NOT_FOUND,
        TREATMENT_NOT_FOUND,
        MEDICATION_INACTIVE,
        INCOMPLETE_TREATMENT,
        INVALID_TRANSITION,
        CARE_LINK_NOT_AUTHORIZED
    }

    private final Code code;

    public TreatmentApplicationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() { return code; }
}
