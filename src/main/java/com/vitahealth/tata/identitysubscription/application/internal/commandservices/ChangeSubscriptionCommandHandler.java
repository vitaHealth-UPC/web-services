package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.commandservices.ChangeSubscriptionCommandService;
import com.vitahealth.tata.identitysubscription.application.internal.SubscriptionMapper;
import com.vitahealth.tata.identitysubscription.application.result.SubscriptionResult;
import com.vitahealth.tata.identitysubscription.domain.model.commands.ChangeSubscriptionCommand;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.domain.services.PlanCatalog;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Changes the account plan while serializing concurrent verification and subscription updates. */
@Service
@Transactional
public class ChangeSubscriptionCommandHandler implements ChangeSubscriptionCommandService {
  private final AccountRepository accountRepository;
  private final Clock clock;

  /** Uses the shared time source for subscription transitions. */
  public ChangeSubscriptionCommandHandler(AccountRepository accountRepository, Clock clock) {
    this.accountRepository = accountRepository;
    this.clock = clock;
  }

  /** Returns expected business failures while allowing technical save failures to roll back. */
  @Override
  public Result<SubscriptionResult, ApplicationError> handle(ChangeSubscriptionCommand command) {
    if (command.accountId() == null || command.accountId().isBlank()) {
      return Result.failure(new ApplicationError("VALIDATION_ERROR", "accountId is required"));
    }
    var candidate = accountRepository.findByIdForUpdate(command.accountId().trim());
    if (candidate.isEmpty()) {
      return Result.failure(new ApplicationError("ACCOUNT_NOT_FOUND", "account not found"));
    }
    var account = candidate.get();
    var selectedPlan = PlanCatalog.findByCode(command.planCode());
    if (selectedPlan.isEmpty()) {
      return Result.failure(new ApplicationError("PLAN_NOT_FOUND", "subscription plan not found"));
    }
    var plan = selectedPlan.get();
    final boolean changed;
    try {
      changed = account.changeSubscription(plan, clock.instant());
    } catch (IllegalStateException exception) {
      return Result.failure(new ApplicationError("ACCOUNT_NOT_ACTIVE", exception.getMessage()));
    }
    if (changed) account = accountRepository.save(account);
    return Result.success(SubscriptionMapper.toResult(account, plan));
  }
}
