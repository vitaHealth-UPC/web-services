package com.vitahealth.tata.identitysubscription.application.internal.queryservices;

import com.vitahealth.tata.identitysubscription.application.internal.SubscriptionMapper;
import com.vitahealth.tata.identitysubscription.application.queryservices.ListAvailablePlansQueryService;
import com.vitahealth.tata.identitysubscription.application.result.PlanResult;
import com.vitahealth.tata.identitysubscription.domain.services.PlanCatalog;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.List;
import org.springframework.stereotype.Service;

/** Projects the domain plan catalog into immutable application results. */
@Service
public class ListAvailablePlansQueryHandler implements ListAvailablePlansQueryService {
  /** Returns catalog projections in domain presentation order without changing account state. */
  @Override
  public Result<List<PlanResult>, ApplicationError> handle() {
    return Result.success(
        PlanCatalog.availablePlans().stream().map(SubscriptionMapper::toResult).toList());
  }
}
