package com.vitahealth.tata.identitysubscription.application.internal.queryservices;

import com.vitahealth.tata.identitysubscription.application.internal.SubscriptionMapper;
import com.vitahealth.tata.identitysubscription.application.queryservices.GetCurrentSubscriptionQueryService;
import com.vitahealth.tata.identitysubscription.application.result.SubscriptionResult;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.domain.services.PlanCatalog;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads current subscription evidence without issuing credentials or changing account state. */
@Service
@Transactional(readOnly = true)
public class GetCurrentSubscriptionQueryHandler implements GetCurrentSubscriptionQueryService {
  private final AccountRepository accountRepository;

  /** Injects account lookup without acquiring mutation locks for this query. */
  public GetCurrentSubscriptionQueryHandler(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  /**
   * Resolves the persisted current plan and returns explicit missing-account or missing-plan
   * failures.
   */
  @Override
  public Result<SubscriptionResult, ApplicationError> handle(String accountId) {
    if (accountId == null || accountId.isBlank()) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "accountId is required"));
    }
    var candidate = accountRepository.findById(accountId);
    if (candidate.isEmpty()) {
      return Result.failure(new ApplicationError("ACCOUNT_NOT_FOUND", "account not found"));
    }
    var account = candidate.get();
    var selectedPlan = PlanCatalog.findByCode(account.currentPlanCode());
    if (selectedPlan.isEmpty()) {
      return Result.failure(new ApplicationError("PLAN_NOT_FOUND", "subscription plan not found"));
    }
    return Result.success(SubscriptionMapper.toResult(account, selectedPlan.get()));
  }
}
