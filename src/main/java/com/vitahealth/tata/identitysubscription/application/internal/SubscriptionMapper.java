package com.vitahealth.tata.identitysubscription.application.internal;

import com.vitahealth.tata.identitysubscription.application.result.PlanResult;
import com.vitahealth.tata.identitysubscription.application.result.SubscriptionResult;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.entities.Plan;
import java.util.Objects;

/** Projects domain catalog and account state into transport-independent application results. */
public final class SubscriptionMapper {
  private SubscriptionMapper() {}

  /** Copies the immutable catalog values without performing billing or persistence work. */
  public static PlanResult toResult(Plan plan) {
    return new PlanResult(
        plan.code(), plan.name(), plan.monthlyPrice(), plan.currency(), plan.capabilities());
  }

  /** Requires the supplied plan to represent the account's current subscription. */
  public static SubscriptionResult toResult(Account account, Plan plan) {
    Objects.requireNonNull(account, "account");
    Objects.requireNonNull(plan, "plan");
    if (!account.currentPlanCode().equals(plan.code())) {
      throw new IllegalArgumentException(
          "Subscription projection must use the account's current plan");
    }
    return new SubscriptionResult(
        account.id(), toResult(plan), account.subscriptionStatus(), account.subscriptionRenewsAt());
  }
}
