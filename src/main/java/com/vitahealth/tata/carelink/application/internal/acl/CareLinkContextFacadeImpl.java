package com.vitahealth.tata.carelink.application.internal.acl;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.queryservices.CareLinkQueryService;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.application.result.CaregiverRecipientPageResult;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.queries.CheckCareRelationshipAuthorizationQuery;
import com.vitahealth.tata.carelink.domain.model.queries.FindCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.FindOlderAdultProfileByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetAuthorizedCaregiversByOlderAdultQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetConfirmedCareLinksByCaregiverQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetOlderAdultProfileByIdQuery;
import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Delegates cross-context reads to Care Link queries without hiding technical failures. */
@Service
public class CareLinkContextFacadeImpl implements CareLinkContextFacade {
  private final CareLinkQueryService queries;

  /** Uses the public query service as the sole owner of relationship/profile reads. */
  public CareLinkContextFacadeImpl(CareLinkQueryService queries) {
    this.queries = queries;
  }

  /** Keeps optional relationship lookup within the public query boundary. */
  @Override
  public Optional<CareLinkResult> findById(String careLinkId) {
    return requireValue(queries.handle(new FindCareLinkByIdQuery(careLinkId)));
  }

  /** Requires relationship evidence through the typed query boundary. */
  @Override
  public CareLinkResult getById(String careLinkId) {
    return requireValue(queries.handle(new GetCareLinkByIdQuery(careLinkId)));
  }

  /** Requires adult profile evidence without querying another context persistence. */
  @Override
  public OlderAdultProfileResult getOlderAdult(String olderAdultId) {
    return requireValue(queries.handle(new GetOlderAdultProfileByIdQuery(olderAdultId)));
  }

  /** Reports current consented caregiver access through the authorization query. */
  @Override
  public boolean isAuthorized(String caregiverId, String olderAdultId) {
    return requireValue(
        queries.handle(new CheckCareRelationshipAuthorizationQuery(caregiverId, olderAdultId)));
  }

  /** Returns confirmed relationship evidence owned by the requested caregiver. */
  @Override
  public List<CareLinkResult> getConfirmedByCaregiver(String caregiverId) {
    return requireValue(queries.handle(new GetConfirmedCareLinksByCaregiverQuery(caregiverId)));
  }

  /** Preserves absent profile evidence independently of technical query failures. */
  @Override
  public Optional<OlderAdultProfileResult> findOlderAdult(String id) {
    return requireValue(queries.handle(new FindOlderAdultProfileByIdQuery(id)));
  }

  /** Translates expected query failures at the ACL boundary without swallowing technical errors. */
  private static <T> T requireValue(Result<T, ApplicationError> result) {
    return switch (result) {
      case Result.Success<T, ApplicationError> success -> success.value();
      case Result.Failure<T, ApplicationError> failure ->
          throw new CareLinkApplicationException(
              CareLinkApplicationException.Code.valueOf(failure.error().code()),
              failure.error().message());
    };
  }

  /** Delegates recipient paging through the same typed public query boundary as authorization. */
  @Override
  public CaregiverRecipientPageResult findAuthorizedCaregivers(
      String olderAdultId, String afterId, int limit) {
    return requireValue(
        queries.handle(new GetAuthorizedCaregiversByOlderAdultQuery(olderAdultId, afterId, limit)));
  }
}
