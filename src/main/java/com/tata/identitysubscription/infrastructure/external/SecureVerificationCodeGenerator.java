package com.tata.identitysubscription.infrastructure.external;

import com.tata.identitysubscription.application.internal.outboundservices.VerificationCodeGenerator;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class SecureVerificationCodeGenerator implements VerificationCodeGenerator {
    private final SecureRandom random = new SecureRandom();
    @Override public String generate() { return "%06d".formatted(random.nextInt(1_000_000)); }
}
