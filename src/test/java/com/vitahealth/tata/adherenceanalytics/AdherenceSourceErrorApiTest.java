package com.vitahealth.tata.adherenceanalytics;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vitahealth.tata.adherenceanalytics.application.commandservices.AdherenceConsolidationCommandService;
import com.vitahealth.tata.adherenceanalytics.application.exceptions.AdherenceSourceUnavailableException;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceSnapshotQueryService;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceViewQueryService;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.AdherenceController;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.AdherenceExceptionHandler;
import com.vitahealth.tata.adherenceanalytics.interfaces.rest.AdherenceViewController;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Verifies source failures are safe server errors across both analytics controller boundaries. */
class AdherenceSourceErrorApiTest {
  @Test
  void providerArgumentFailuresCannotBecomeClientValidationErrors() throws Exception {
    var queries = mock(AdherenceQueryService.class);
    var views = mock(AdherenceViewQueryService.class);
    var failure =
        new AdherenceSourceUnavailableException(
            new IllegalArgumentException("private source details"));
    when(queries.weekly(any())).thenThrow(failure);
    when(views.summary(any())).thenThrow(failure);
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("i18n/messages");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new AdherenceController(
                    queries,
                    mock(AdherenceConsolidationCommandService.class),
                    mock(AdherenceSnapshotQueryService.class)),
                new AdherenceViewController(views, Clock.systemUTC()))
            .setControllerAdvice(new AdherenceExceptionHandler(messages))
            .build();
    for (var path :
        new String[] {
          "weekly?from=2026-10-01T00:00:00Z&to=2026-10-08T00:00:00Z", "summary?days=7"
        }) {
      mvc.perform(
              get("/api/v1/older-adults/adult/adherence/" + path).header("Accept-Language", "en"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.title").value("ADHERENCE_SOURCE_ERROR"))
          .andExpect(jsonPath("$.detail").value("Unable to retrieve adherence evidence"));
    }
    mvc.perform(get("/api/v1/older-adults/adult/adherence/summary?days=0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
    for (var path :
        new String[] {
          "weekly",
          "weekly?from=private-conversion-input&to=2026-10-08T00:00:00Z",
          "summary?days=private-number-input",
          "summary?zone=private-zone-input"
        }) {
      mvc.perform(
              get("/api/v1/older-adults/adult/adherence/" + path).header("Accept-Language", "en"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
          .andExpect(
              jsonPath("$.detail").value("Review the adherence period and calendar parameters"));
    }
    mvc.perform(get("/api/v1/older-adults/adult/adherence/weekly").header("Accept-Language", "es"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("Revisa el período y los parámetros de calendario de adherencia"));
  }
}
