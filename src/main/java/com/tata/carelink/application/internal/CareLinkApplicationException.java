package com.tata.carelink.application.internal;

public final class CareLinkApplicationException extends RuntimeException {
    public enum Code {
        ACCOUNT_NOT_ENABLED,
        OLDER_ADULT_NOT_FOUND,
        CARE_LINK_NOT_FOUND,
        INVALID_LINKING_CODE,
        LINKING_CODE_EXPIRED_OR_USED,
        CONSENT_REQUIRED,
        CARE_LINK_REVOKED
    }

    private final Code code;

    public CareLinkApplicationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() { return code; }
}
