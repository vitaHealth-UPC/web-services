package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.CareLinkApplicationException.Code;
import com.vitahealth.tata.carelink.interfaces.rest.CareLinkExceptionHandler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.CareLinkProblemDetailFromErrorAssembler;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Verifies complete bilingual business catalogs, real request language resolution and privacy. */
@SpringBootTest
@ActiveProfiles("test")
class CareLinkErrorLocalizationIntegrationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private CareLinkExceptionHandler failures;
  @Autowired private CareLinkProblemDetailFromErrorAssembler problems;

  @AfterEach
  void restoreLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  @ParameterizedTest
  @EnumSource(Code.class)
  void everyBusinessFailureHasTwoPublicMessagesWithoutChangingItsCode(Code code) {
    var failure = new CareLinkApplicationException(code, "PRIVATE-DIAGNOSTIC");
    LocaleContextHolder.setLocale(Locale.forLanguageTag("es-419"));
    var spanish = failures.handleCareLink(failure);
    LocaleContextHolder.setLocale(Locale.US);
    var english = failures.handleCareLink(failure);
    assertThat(spanish.getTitle()).isEqualTo(code.name());
    assertThat(english.getTitle()).isEqualTo(code.name());
    assertThat(spanish.getDetail()).isNotBlank().doesNotContain("PRIVATE-DIAGNOSTIC");
    assertThat(english.getDetail()).isNotBlank().doesNotContain("PRIVATE-DIAGNOSTIC");
    assertThat(spanish.getDetail()).isNotEqualTo(english.getDetail());
    assertThat(spanish.getStatus()).isEqualTo(english.getStatus());
  }

  static Stream<Arguments> requestLanguages() {
    var spanish = "El código de vinculación es inválido";
    var english = "Linking code is invalid";
    return Stream.of(
        Arguments.of("es-419", spanish),
        Arguments.of("es-PE", spanish),
        Arguments.of("en-US", english),
        Arguments.of("fr", english),
        Arguments.of(null, english));
  }

  @ParameterizedTest
  @MethodSource("requestLanguages")
  void actualAcceptanceFailureUsesRequestLanguageAndStableCode(String language, String message)
      throws Exception {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    var request =
        post("/api/v1/care-links/acceptances")
            .header("Authorization", "Bearer " + fixture.caregiverToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"caregiverId\":\"" + fixture.caregiverId() + "\",\"code\":\"UNKNOWN-CODE\"}");
    if (language != null) request.header("Accept-Language", language);
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("INVALID_LINKING_CODE"))
        .andExpect(jsonPath("$.detail").value(message));
  }

  @Test
  void invalidSubmittedFieldsReceiveGenericLocalizedValidationWithoutEcho() throws Exception {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var secret = "private-linking-code-that-exceeds-the-allowed-field-length";
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    var response =
        mvc.perform(
                post("/api/v1/care-links/acceptances")
                    .header("Authorization", "Bearer " + fixture.caregiverToken())
                    .header("Accept-Language", "es-419")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"caregiverId\":\""
                            + fixture.caregiverId()
                            + "\",\"code\":\""
                            + secret
                            + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.detail").value("Los datos de la solicitud son inválidos"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response).doesNotContain(secret);
  }

  @Test
  void diagnosticDetailsNeverBecomeProblemResponseProperties() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
    var problem =
        problems.toResource(
            HttpStatus.BAD_REQUEST,
            new ApplicationError(
                "INVALID_LINKING_CODE", "public fallback", "private SQL and token"));
    assertThat(problem.toString()).doesNotContain("private SQL and token");
    assertThat(problem.getProperties()).isNull();
  }
}
