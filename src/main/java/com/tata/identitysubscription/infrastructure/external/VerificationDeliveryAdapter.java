package com.tata.identitysubscription.infrastructure.external;

import com.tata.identitysubscription.application.internal.outboundservices.VerificationDeliveryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VerificationDeliveryAdapter implements VerificationDeliveryPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationDeliveryAdapter.class);

    @Override
    public void send(String email, String code) {
        // The external e-mail provider plugs in here. Raw codes are intentionally not logged.
        LOGGER.info("Verification message queued for {}", email);
    }
}
