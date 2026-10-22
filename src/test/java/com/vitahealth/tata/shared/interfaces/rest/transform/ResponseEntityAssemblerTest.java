package com.vitahealth.tata.shared.interfaces.rest.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Verifies that result translation preserves success resources and Tata's failure contract. */
class ResponseEntityAssemblerTest {
  @Test
  void mapsSuccessWithoutInvokingFailureMapper() {
    var response =
        ResponseEntityAssembler.toResponseEntityFromResult(
            Result.<String, ApplicationError>success("value"),
            value -> "resource:" + value,
            HttpStatus.CREATED,
            error -> {
              throw new AssertionError("unexpected failure mapping");
            });

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isEqualTo("resource:value");
  }

  @Test
  void preservesContextStatusAndHidesInternalDetails() {
    var error = new ApplicationError("CARE_LINK_REQUIRED", "Vínculo requerido", "internal SQL");
    var response =
        ResponseEntityAssembler.toResponseEntityFromResult(
            Result.<String, ApplicationError>failure(error),
            value -> {
              throw new AssertionError("unexpected success mapping");
            },
            HttpStatus.OK,
            failure -> HttpStatus.FORBIDDEN);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    var problem = (ProblemDetail) response.getBody();
    assertThat(problem.getTitle()).isEqualTo("CARE_LINK_REQUIRED");
    assertThat(problem.getDetail()).isEqualTo("Vínculo requerido");
    assertThat(problem.getProperties()).isNull();
  }

  @Test
  void rejectsSuccessStatusForFailure() {
    assertThatThrownBy(
            () ->
                ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    new ApplicationError("FAILED", "Falló"), HttpStatus.OK))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
