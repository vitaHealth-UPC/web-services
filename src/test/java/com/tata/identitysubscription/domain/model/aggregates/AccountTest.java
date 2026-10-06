package com.tata.identitysubscription.domain.model.aggregates;

import com.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    @Test
    void newAccountStartsPendingAndCanBeVerifiedBeforeExpiry() {
        var now = Instant.parse("2026-10-05T12:00:00Z");
        var account = Account.register(
                "Diego Mendoza",
                new EmailAddress("diego@example.com"),
                "password-hash",
                "verification-hash",
                now,
                Duration.ofMinutes(15)
        );

        assertEquals(AccountStatus.PENDING_VERIFICATION, account.status());
        assertTrue(account.canCompleteVerification(now.plusSeconds(30)));
        account.completeVerification();
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertTrue(account.canAuthenticate());
        assertNull(account.verificationCodeHash());
    }

    @Test
    void verificationExpiresAtBoundary() {
        var now = Instant.parse("2026-10-05T12:00:00Z");
        var account = Account.register(
                "Diego Mendoza",
                new EmailAddress("diego@example.com"),
                "password-hash",
                "verification-hash",
                now,
                Duration.ofMinutes(15)
        );
        assertFalse(account.canCompleteVerification(now.plus(Duration.ofMinutes(15))));
    }
}
