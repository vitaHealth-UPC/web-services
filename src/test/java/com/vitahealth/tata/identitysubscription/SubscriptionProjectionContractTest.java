package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.identitysubscription.application.internal.SubscriptionMapper;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.services.PlanCatalog;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Ensures subscription projections cannot combine an account with another plan's capabilities. */
class SubscriptionProjectionContractTest {
  @Test
  void rejectsPlanMismatchAndPreservesCurrentPlan() {
    var account =
        Account.register(
            "Rosa",
            new EmailAddress("rosa@example.com"),
            "hash",
            "challenge",
            Instant.now(),
            Duration.ofMinutes(15));
    var family = PlanCatalog.findByCode(PlanCatalog.FAMILY).orElseThrow();
    assertThrows(
        IllegalArgumentException.class, () -> SubscriptionMapper.toResult(account, family));
    var essential = PlanCatalog.findByCode(PlanCatalog.ESSENTIAL).orElseThrow();
    assertEquals(
        PlanCatalog.ESSENTIAL, SubscriptionMapper.toResult(account, essential).plan().code());
  }
}
