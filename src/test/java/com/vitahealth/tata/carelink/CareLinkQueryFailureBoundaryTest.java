package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.queryservices.CareLinkQueryServiceImpl;
import com.vitahealth.tata.carelink.application.internal.queryservices.CheckCareRelationshipAuthorizationQueryHandler;
import com.vitahealth.tata.carelink.application.internal.queryservices.FindCareLinkByIdQueryHandler;
import com.vitahealth.tata.carelink.application.internal.queryservices.FindOlderAdultProfileByIdQueryHandler;
import com.vitahealth.tata.carelink.application.internal.queryservices.GetAuthorizedCaregiversByOlderAdultQueryHandler;
import com.vitahealth.tata.carelink.application.internal.queryservices.GetCareLinkByIdQueryHandler;
import com.vitahealth.tata.carelink.application.internal.queryservices.GetConfirmedCareLinkSummariesQueryHandler;
import com.vitahealth.tata.carelink.application.internal.queryservices.GetConfirmedCareLinksByCaregiverQueryHandler;
import com.vitahealth.tata.carelink.application.internal.queryservices.GetOlderAdultProfileByIdQueryHandler;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.queries.GetCareLinkByIdQuery;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

/** Verifies the public Result boundary translates only context-owned expected read failures. */
class CareLinkQueryFailureBoundaryTest {
  private final GetCareLinkByIdQueryHandler handler = mock(GetCareLinkByIdQueryHandler.class);
  private final CareLinkQueryServiceImpl queries =
      new CareLinkQueryServiceImpl(
          mock(FindOlderAdultProfileByIdQueryHandler.class),
          mock(FindCareLinkByIdQueryHandler.class),
          handler,
          mock(GetOlderAdultProfileByIdQueryHandler.class),
          mock(GetConfirmedCareLinksByCaregiverQueryHandler.class),
          mock(CheckCareRelationshipAuthorizationQueryHandler.class),
          mock(GetConfirmedCareLinkSummariesQueryHandler.class),
          mock(GetAuthorizedCaregiversByOlderAdultQueryHandler.class));

  @Test
  void expectedMissingRelationshipBecomesFailureWithItsOriginalCode() {
    var query = new GetCareLinkByIdQuery("missing");
    when(handler.handle(query))
        .thenThrow(
            new CareLinkApplicationException(
                CareLinkApplicationException.Code.CARE_LINK_NOT_FOUND, "care link not found"));
    var result = queries.handle(query);
    assertThat(result.isFailure()).isTrue();
    var failure = (Result.Failure<CareLinkResult, ApplicationError>) result;
    assertThat(failure.error().code()).isEqualTo("CARE_LINK_NOT_FOUND");
  }

  @Test
  void databaseOutagePropagatesInsteadOfBecomingMissingEvidence() {
    var query = new GetCareLinkByIdQuery("link");
    var failure = new DataAccessResourceFailureException("database unavailable");
    when(handler.handle(query)).thenThrow(failure);
    assertSame(
        failure,
        assertThrows(DataAccessResourceFailureException.class, () -> queries.handle(query)));
  }
}
