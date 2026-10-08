package com.tata.intakeexecution.application;

/** Public application failure contract; adapters translate its codes to transport responses. */
public final class IntakeApplicationException extends RuntimeException {
    public enum Code {
        INTAKE_NOT_FOUND,
        INTAKE_NOT_CONFIRMABLE,
        VOICE_CONFIRMATION_DISABLED
    }

    private final Code code;

    public IntakeApplicationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
