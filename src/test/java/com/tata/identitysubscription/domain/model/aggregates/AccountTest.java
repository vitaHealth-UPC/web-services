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
    void renewVerificationCodeReplacesHashAndExtendsExpiry() {
        var now = Instant.parse("2026-10-05T12:00:00Z");
        var account = Account.register(
                "Diego Mendoza",
                new EmailAddress("diego@example.com"),
                "password-hash",
                "old-verification-hash",
                now,
                Duration.ofMinutes(15)
        );

        var renewedAt = now.plus(Duration.ofMinutes(20));
        account.renewVerificationCode(
                "new-verification-hash",
                renewedAt,
                Duration.ofMinutes(15)
        );

        assertEquals("new-verification-hash", account.verificationCodeHash());
        assertEquals(renewedAt.plus(Duration.ofMinutes(15)), account.verificationExpiresAt());
        assertTrue(account.canCompleteVerification(renewedAt.plusSeconds(30)));
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

    @Test
    void activeAccountCanChangePlanWithoutExtendingRenewalOnRetry() {
        var now = Instant.parse("2026-10-06T12:00:00Z");
        var account = Account.rehydrate(
                "account-1",
                "Diego Mendoza",
                new EmailAddress("diego@example.com"),
                "password-hash",
                AccountStatus.ACTIVE,
                null,
                null,
                com.tata.identitysubscription.domain.services.PlanCatalog.ESSENTIAL,
                com.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus.ACTIVE,
                now.plus(Duration.ofDays(10))
        );
        var family = com.tata.identitysubscription.domain.services.PlanCatalog.findByCode("FAMILY").orElseThrow();

        assertTrue(account.changeSubscription(family, now));
        var renewsAt = account.subscriptionRenewsAt();
        assertEquals("FAMILY", account.currentPlanCode());

        assertFalse(account.changeSubscription(family, now.plusSeconds(30)));
        assertEquals(renewsAt, account.subscriptionRenewsAt());
    }
}
