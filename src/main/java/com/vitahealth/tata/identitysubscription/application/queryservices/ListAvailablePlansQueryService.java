package com.vitahealth.tata.identitysubscription.application.queryservices;

import com.vitahealth.tata.identitysubscription.application.result.PlanResult;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.List;

/** Read contract for the available product plans, independent of HTTP representation. */
public interface ListAvailablePlansQueryService {
  /** Returns an immutable plan catalog as an explicit successful result. */
  Result<List<PlanResult>, ApplicationError> handle();
}
