package com.tata.carelink.infrastructure.external;

import com.tata.carelink.application.internal.outboundservices.LinkingCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureLinkingCodeGenerator implements LinkingCodeGenerator {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return "TATA-%04d".formatted(random.nextInt(10_000));
    }
}
