package com.vitahealth.tata.carelink.application.queryservices;

import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.application.result.CaregiverRecipientPageResult;
import com.vitahealth.tata.carelink.application.result.ConfirmedCareLinkResult;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.queries.CheckCareRelationshipAuthorizationQuery;
import com.vitahealth.tata.carelink.domain.model.queries.FindCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.FindOlderAdultProfileByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetAuthorizedCaregiversByOlderAdultQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetConfirmedCareLinkSummariesQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetConfirmedCareLinksByCaregiverQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetOlderAdultProfileByIdQuery;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.List;
import java.util.Optional;

/**
 * Public relationship/profile reads whose implementations own projection assembly and persistence.
 */
public interface CareLinkQueryService {
  /** Returns profile absence directly while preserving infrastructure failures. */
  Result<Optional<OlderAdultProfileResult>, ApplicationError> handle(
      FindOlderAdultProfileByIdQuery query);

  /** Looks up a relationship whose absence is expected, preserving technical failures. */
  Result<Optional<CareLinkResult>, ApplicationError> handle(FindCareLinkByIdQuery query);

  /** Requires the requested relationship evidence. */
  Result<CareLinkResult, ApplicationError> handle(GetCareLinkByIdQuery query);

  /** Requires the requested adult profile evidence. */
  Result<OlderAdultProfileResult, ApplicationError> handle(GetOlderAdultProfileByIdQuery query);

  /** Checks current consented relationship access without creating or changing a link. */
  Result<Boolean, ApplicationError> handle(CheckCareRelationshipAuthorizationQuery query);

  /** Lists full authorized relationship results for cross-context consumers. */
  Result<List<CareLinkResult>, ApplicationError> handle(
      GetConfirmedCareLinksByCaregiverQuery query);

  /** Lists credential-free display projections with profile data loaded in one batch. */
  Result<List<ConfirmedCareLinkResult>, ApplicationError> handle(
      GetConfirmedCareLinkSummariesQuery query);

  /** Scans authorized caregiver recipients without exposing linking-code evidence. */
  Result<CaregiverRecipientPageResult, ApplicationError> handle(
      GetAuthorizedCaregiversByOlderAdultQuery query);
}
