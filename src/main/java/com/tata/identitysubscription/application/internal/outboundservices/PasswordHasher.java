package com.tata.identitysubscription.application.internal.outboundservices;

public interface PasswordHasher {
    String hash(String rawValue);
    boolean matches(String rawValue, String encodedValue);
}
