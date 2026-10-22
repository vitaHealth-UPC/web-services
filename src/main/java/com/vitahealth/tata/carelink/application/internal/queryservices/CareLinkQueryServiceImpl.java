package com.vitahealth.tata.carelink.application.internal.queryservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.queryservices.CareLinkQueryService;
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
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** Delegates typed public reads to handlers that own their individual read-only boundaries. */
@Service
public class CareLinkQueryServiceImpl implements CareLinkQueryService {
  private final FindOlderAdultProfileByIdQueryHandler findOlderAdultProfileByIdQueryHandler;
  private final FindCareLinkByIdQueryHandler findCareLinkByIdQueryHandler;
  private final GetCareLinkByIdQueryHandler getCareLinkByIdQueryHandler;
  private final GetOlderAdultProfileByIdQueryHandler getOlderAdultProfileByIdQueryHandler;
  private final GetConfirmedCareLinksByCaregiverQueryHandler
      getConfirmedCareLinksByCaregiverQueryHandler;
  private final CheckCareRelationshipAuthorizationQueryHandler
      checkCareRelationshipAuthorizationQueryHandler;
  private final GetConfirmedCareLinkSummariesQueryHandler getConfirmedCareLinkSummariesQueryHandler;

  private final GetAuthorizedCaregiversByOlderAdultQueryHandler caregiverRecipients;

  /** Injects concrete query handlers without mixing their persistence responsibilities. */
  public CareLinkQueryServiceImpl(
      FindOlderAdultProfileByIdQueryHandler findOlderAdultProfileByIdQueryHandler,
      FindCareLinkByIdQueryHandler findCareLinkByIdQueryHandler,
      GetCareLinkByIdQueryHandler getCareLinkByIdQueryHandler,
      GetOlderAdultProfileByIdQueryHandler getOlderAdultProfileByIdQueryHandler,
      GetConfirmedCareLinksByCaregiverQueryHandler getConfirmedCareLinksByCaregiverQueryHandler,
      CheckCareRelationshipAuthorizationQueryHandler checkCareRelationshipAuthorizationQueryHandler,
      GetConfirmedCareLinkSummariesQueryHandler getConfirmedCareLinkSummariesQueryHandler,
      GetAuthorizedCaregiversByOlderAdultQueryHandler caregiverRecipients) {
    this.caregiverRecipients = caregiverRecipients;
    this.findOlderAdultProfileByIdQueryHandler = findOlderAdultProfileByIdQueryHandler;
    this.findCareLinkByIdQueryHandler = findCareLinkByIdQueryHandler;
    this.getCareLinkByIdQueryHandler = getCareLinkByIdQueryHandler;
    this.getOlderAdultProfileByIdQueryHandler = getOlderAdultProfileByIdQueryHandler;
    this.getConfirmedCareLinksByCaregiverQueryHandler =
        getConfirmedCareLinksByCaregiverQueryHandler;
    this.checkCareRelationshipAuthorizationQueryHandler =
        checkCareRelationshipAuthorizationQueryHandler;
    this.getConfirmedCareLinkSummariesQueryHandler = getConfirmedCareLinkSummariesQueryHandler;
  }

  /** Reads optional profile evidence through its dedicated read-only handler. */
  @Override
  public Result<Optional<OlderAdultProfileResult>, ApplicationError> handle(
      FindOlderAdultProfileByIdQuery query) {
    return read(() -> findOlderAdultProfileByIdQueryHandler.handle(query));
  }

  /**
   * Reads a relationship while preserving the distinction between absence and technical failure.
   */
  @Override
  public Result<Optional<CareLinkResult>, ApplicationError> handle(FindCareLinkByIdQuery query) {
    return read(() -> findCareLinkByIdQueryHandler.handle(query));
  }

  /** Loads one required relationship projection. */
  @Override
  public Result<CareLinkResult, ApplicationError> handle(GetCareLinkByIdQuery query) {
    return read(() -> getCareLinkByIdQueryHandler.handle(query));
  }

  /** Loads one required adult profile projection. */
  @Override
  public Result<OlderAdultProfileResult, ApplicationError> handle(
      GetOlderAdultProfileByIdQuery query) {
    return read(() -> getOlderAdultProfileByIdQueryHandler.handle(query));
  }

  /** Loads full confirmed relationship evidence for context consumers. */
  @Override
  public Result<List<CareLinkResult>, ApplicationError> handle(
      GetConfirmedCareLinksByCaregiverQuery query) {
    return read(() -> getConfirmedCareLinksByCaregiverQueryHandler.handle(query));
  }

  /** Reads current confirmed consent authorization for a member pair. */
  @Override
  public Result<Boolean, ApplicationError> handle(CheckCareRelationshipAuthorizationQuery query) {
    return read(() -> checkCareRelationshipAuthorizationQueryHandler.handle(query));
  }

  /** Loads credential-free display summaries through batched profile reads. */
  @Override
  public Result<List<ConfirmedCareLinkResult>, ApplicationError> handle(
      GetConfirmedCareLinkSummariesQuery query) {
    return read(() -> getConfirmedCareLinkSummariesQueryHandler.handle(query));
  }

  /** Converts expected read rejections after the handler boundary; technical failures propagate. */
  private static <T> Result<T, ApplicationError> read(Supplier<T> query) {
    try {
      return Result.success(query.get());
    } catch (CareLinkApplicationException failure) {
      return Result.failure(failure.toApplicationError());
    }
  }

  /** Returns a bounded credential-free caregiver recipient page through its read-only handler. */
  @Override
  public Result<CaregiverRecipientPageResult, ApplicationError> handle(
      GetAuthorizedCaregiversByOlderAdultQuery query) {
    return read(() -> caregiverRecipients.handle(query));
  }
}
