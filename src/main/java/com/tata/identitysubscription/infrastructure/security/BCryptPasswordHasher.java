package com.tata.identitysubscription.infrastructure.security;

import com.tata.identitysubscription.application.internal.outboundservices.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override public String hash(String rawValue) { return encoder.encode(rawValue); }
    @Override public boolean matches(String rawValue, String encodedValue) {
        return rawValue != null && encodedValue != null && encoder.matches(rawValue, encodedValue);
    }
}
