package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.carelink.application.queryservices.CareLinkQueryService;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.queries.FindCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.FindOlderAdultProfileByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetOlderAdultProfileByIdQuery;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Verifies absent optional evidence and missing required evidence are distinct typed outcomes. */
@SpringBootTest
@ActiveProfiles("test")
class CareLinkQueryResultIntegrationTest {
  @Autowired private CareLinkQueryService queries;

  @Test
  void optionalAbsenceIsSuccessfulButRequiredAbsenceIsFailure() {
    var id = UUID.randomUUID().toString();
    var optional = queries.handle(new FindCareLinkByIdQuery(id));
    assertThat(optional.isSuccess()).isTrue();
    assertThat(optional.toOptional().orElseThrow()).isEmpty();
    var required = queries.handle(new GetCareLinkByIdQuery(id));
    assertThat(required.isFailure()).isTrue();
    var failure = (Result.Failure<CareLinkResult, ApplicationError>) required;
    assertThat(failure.error().code()).isEqualTo("CARE_LINK_NOT_FOUND");
  }

  @Test
  void profileAbsencePreservesTheSameOptionalVersusRequiredDistinction() {
    var id = UUID.randomUUID().toString();
    var optional = queries.handle(new FindOlderAdultProfileByIdQuery(id));
    assertThat(optional.isSuccess()).isTrue();
    assertThat(optional.toOptional().orElseThrow()).isEmpty();
    var required = queries.handle(new GetOlderAdultProfileByIdQuery(id));
    assertThat(required.isFailure()).isTrue();
    var failure = (Result.Failure<OlderAdultProfileResult, ApplicationError>) required;
    assertThat(failure.error().code()).isEqualTo("OLDER_ADULT_NOT_FOUND");
  }
}
