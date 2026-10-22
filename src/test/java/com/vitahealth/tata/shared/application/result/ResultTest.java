package com.vitahealth.tata.shared.application.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.shared.interfaces.rest.transform.ProblemDetailAssembler;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Checks composition and HTTP adaptation of reusable application results. */
class ResultTest {
  @Test
  void compositionCannotEscapeTheResultContractWithNull() {
    Result<Integer, String> successful = Result.success(1);
    Result<Integer, String> failed = Result.failure("missing");
    assertThrows(NullPointerException.class, () -> successful.flatMap(value -> null));
    assertThrows(NullPointerException.class, () -> failed.recover(error -> null));
    for (Result<Integer, String> result : List.of(successful, failed)) {
      assertThrows(NullPointerException.class, () -> result.map(null));
      assertThrows(NullPointerException.class, () -> result.flatMap(null));
      assertThrows(NullPointerException.class, () -> result.mapError(null));
      assertThrows(NullPointerException.class, () -> result.recover(null));
    }
  }

  @Test
  void publicFailureEvidenceRequiresAnErrorStatusAndNonblankMessage() {
    assertThrows(NullPointerException.class, () -> new ApplicationError(null, "message"));
    assertThrows(NullPointerException.class, () -> new ApplicationError("CODE", null));
    assertThrows(IllegalArgumentException.class, () -> new ApplicationError(" ", "message"));
    assertThrows(IllegalArgumentException.class, () -> new ApplicationError("CODE", "\u2003"));
    assertThrows(
        IllegalArgumentException.class,
        () -> ProblemDetailAssembler.from(HttpStatus.OK, "CODE", "message"));
    assertThrows(
        IllegalArgumentException.class,
        () -> ProblemDetailAssembler.from(HttpStatus.FOUND, "CODE", "message"));
  }

  @Test
  void rejectsMissingValuesAtConstruction() {
    assertThrows(NullPointerException.class, () -> Result.success(null));
    assertThrows(NullPointerException.class, () -> Result.failure(null));
    assertThrows(NullPointerException.class, () -> new Result.Success<>(null));
    assertThrows(NullPointerException.class, () -> new Result.Failure<>(null));
    Result<Integer, String> value = Result.success(1);
    assertThrows(NullPointerException.class, () -> value.map(number -> null));
  }

  @Test
  void successfulCompositionTransformsValues() {
    Result<Integer, String> start = Result.success(3);
    var result = start.map(value -> value * 2).flatMap(value -> Result.success(value + 1));
    assertEquals(7, result.toOptional().orElseThrow());
    assertTrue(result.isSuccess());
  }

  @Test
  void failedCompositionSkipsSuccessFunctionsAndCanRecover() {
    Result<Integer, String> failed = Result.failure("missing");
    var mapped =
        failed.map(
            value -> {
              throw new AssertionError("must not map failure");
            });
    assertTrue(mapped.isFailure());
    assertTrue(mapped.toOptional().isEmpty());
    assertEquals(
        "MISSING",
        ((Result.Failure<Integer, String>) failed.mapError(String::toUpperCase)).error());
    assertEquals(9, failed.recover(error -> Result.success(9)).getOrElse(0));
    assertEquals(0, failed.getOrElse(0));
  }

  @Test
  void errorCodesRemainStableAcrossServerLocales() {
    var previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      assertEquals("INTAKE_NOT_FOUND", ApplicationError.notFound("intake", "id").code());
      assertEquals("INVENTORY_CONFLICT", ApplicationError.conflict("inventory", "changed").code());
    } finally {
      Locale.setDefault(previous);
    }
  }

  @Test
  void applicationErrorPreservesExistingProblemDetailContract() {
    var detail =
        ProblemDetailAssembler.from(
            HttpStatus.CONFLICT,
            new ApplicationError("CONCURRENT_UPDATE", "Please retry", "internal context"));
    assertEquals(409, detail.getStatus());
    assertEquals("CONCURRENT_UPDATE", detail.getTitle());
    assertEquals("Please retry", detail.getDetail());
    assertNull(detail.getProperties());
  }
}
