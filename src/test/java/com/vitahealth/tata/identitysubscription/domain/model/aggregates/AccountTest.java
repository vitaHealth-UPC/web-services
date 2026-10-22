package com.vitahealth.tata.identitysubscription.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.SubscriptionStatus;
import com.vitahealth.tata.identitysubscription.domain.services.PlanCatalog;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Checks account activation, challenge renewal and atomic subscription transitions. */
class AccountTest {
  @Test
  void newAccountStartsPendingAndCanBeVerifiedBeforeExpiry() {
    var now = Instant.parse("2026-10-05T12:00:00Z");
    var account =
        Account.register(
            "Diego Mendoza",
            new EmailAddress("diego@example.com"),
            "password-hash",
            "verification-hash",
            now,
            Duration.ofMinutes(15));

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
    var account =
        Account.register(
            "Diego Mendoza",
            new EmailAddress("diego@example.com"),
            "password-hash",
            "old-verification-hash",
            now,
            Duration.ofMinutes(15));

    var renewedAt = now.plus(Duration.ofMinutes(20));
    account.renewVerificationCode("new-verification-hash", renewedAt, Duration.ofMinutes(15));

    assertEquals("new-verification-hash", account.verificationCodeHash());
    assertEquals(renewedAt.plus(Duration.ofMinutes(15)), account.verificationExpiresAt());
    assertTrue(account.canCompleteVerification(renewedAt.plusSeconds(30)));
  }

  @Test
  void verificationExpiresAtBoundary() {
    var now = Instant.parse("2026-10-05T12:00:00Z");
    var account =
        Account.register(
            "Diego Mendoza",
            new EmailAddress("diego@example.com"),
            "password-hash",
            "verification-hash",
            now,
            Duration.ofMinutes(15));
    assertFalse(account.canCompleteVerification(now.plus(Duration.ofMinutes(15))));
  }

  @Test
  void activeAccountCanChangePlanWithoutExtendingRenewalOnRetry() {
    var now = Instant.parse("2026-10-06T12:00:00Z");
    var account =
        Account.rehydrate(
            "account-1",
            "Diego Mendoza",
            new EmailAddress("diego@example.com"),
            "password-hash",
            AccountStatus.ACTIVE,
            null,
            null,
            PlanCatalog.ESSENTIAL,
            SubscriptionStatus.ACTIVE,
            now.plus(Duration.ofDays(10)));
    var family = PlanCatalog.findByCode("FAMILY").orElseThrow();

    assertTrue(account.changeSubscription(family, now));
    var renewsAt = account.subscriptionRenewsAt();
    assertEquals("FAMILY", account.currentPlanCode());

    assertFalse(account.changeSubscription(family, now.plusSeconds(30)));
    assertEquals(renewsAt, account.subscriptionRenewsAt());
  }

  @Test
  void unrepresentableRenewalLeavesSubscriptionStateIntact() {
    var renewal = Instant.parse("2026-11-01T12:00:00Z");
    var account =
        Account.rehydrate(
            "account",
            "Rosa",
            new EmailAddress("rosa@example.com"),
            "password-hash",
            AccountStatus.ACTIVE,
            null,
            null,
            PlanCatalog.ESSENTIAL,
            SubscriptionStatus.ACTIVE,
            renewal);
    var family = PlanCatalog.findByCode("FAMILY").orElseThrow();
    assertThrows(DateTimeException.class, () -> account.changeSubscription(family, Instant.MAX));
    assertEquals(PlanCatalog.ESSENTIAL, account.currentPlanCode());
    assertEquals(SubscriptionStatus.ACTIVE, account.subscriptionStatus());
    assertEquals(renewal, account.subscriptionRenewsAt());
  }
}
