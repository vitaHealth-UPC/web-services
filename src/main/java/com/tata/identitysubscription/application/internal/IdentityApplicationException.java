package com.tata.identitysubscription.application.internal;

public final class IdentityApplicationException extends RuntimeException {
    public enum Code {
        DUPLICATE_EMAIL,
        ACCOUNT_NOT_FOUND,
        INVALID_VERIFICATION,
        VERIFICATION_EXPIRED,
        ACCOUNT_NOT_ACTIVE,
        INVALID_CREDENTIALS,
        PIN_ALREADY_REGISTERED,
        PIN_NOT_FOUND,
        INVALID_PIN,
        PIN_LOCKED
    }

    private final Code code;

    public IdentityApplicationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() { return code; }
}
