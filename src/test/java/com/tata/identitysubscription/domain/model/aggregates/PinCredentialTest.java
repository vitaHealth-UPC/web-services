package com.tata.identitysubscription.domain.model.aggregates;

import com.tata.identitysubscription.domain.model.valueobjects.PinPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PinCredentialTest {
    @Test
    void locksAfterConfiguredFailedAttemptsAndResetsAfterSuccess() {
        var now = Instant.parse("2026-10-05T12:00:00Z");
        var policy = new PinPolicy(4, Duration.ofMinutes(15));
        var credential = PinCredential.register("older-adult-1", "hash");

        for (int i = 0; i < 4; i++) {
            credential.registerFailure(now, policy);
        }

        assertTrue(credential.isLockedAt(now.plusSeconds(1)));
        assertEquals(4, credential.failedAttempts());

        credential.registerSuccess();

        assertFalse(credential.isLockedAt(now.plusSeconds(1)));
        assertEquals(0, credential.failedAttempts());
    }

    @Test
    void lockExpiresAfterPolicyDuration() {
        var now = Instant.parse("2026-10-05T12:00:00Z");
        var policy = new PinPolicy(1, Duration.ofMinutes(15));
        var credential = PinCredential.register("older-adult-1", "hash");

        credential.registerFailure(now, policy);

        assertTrue(credential.isLockedAt(now.plusSeconds(30)));
        assertFalse(credential.isLockedAt(now.plus(Duration.ofMinutes(15))));
    }
}
