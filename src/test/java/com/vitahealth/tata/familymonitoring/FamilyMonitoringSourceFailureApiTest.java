package com.vitahealth.tata.familymonitoring;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitahealth.tata.adherenceanalytics.interfaces.acl.AdherenceContextFacade;
import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.FamilyMonitorSelectionService;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Exercises HTTP error classification with real consent/session state and controlled source faults.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FamilyMonitoringSourceFailureApiTest {
  @Autowired WebApplicationContext context;
  @Autowired CareLinkRepository links;
  @Autowired CareLinkCommandService consent;
  @Autowired AccountRepository accounts;
  @Autowired SessionTokenService tokens;
  @Autowired Clock clock;

  @MockitoBean(enforceOverride = true)
  AdherenceContextFacade analytics;

  @MockitoBean(enforceOverride = true)
  IntakeContextFacade intakes;

  @MockitoSpyBean FamilyMonitorSelectionService selection;
  private MockMvc mvc;
  private String owner;
  private String caregiver;

  @BeforeEach
  void confirmedCaregiver() {
    owner = UUID.randomUUID().toString();
    caregiver = UUID.randomUUID().toString();
    accounts.save(
        Account.rehydrate(
            caregiver,
            "Caregiver",
            new EmailAddress(caregiver + "@example.com"),
            "hash",
            AccountStatus.ACTIVE,
            null,
            null));
    var now = clock.instant();
    var code = UUID.randomUUID().toString().replace("-", "");
    var link =
        CareLink.createPending(
            caregiver, owner, LinkingCode.issue(code, now, Duration.ofMinutes(15)), now);
    link.accept(code, now);
    links.save(link);
    consent.registerConsent(new RegisterConsentCommand(link.id(), true)).toOptional().orElseThrow();
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(
                get("/").header("Authorization", "Bearer " + tokens.issue(caregiver).accessToken()))
            .build();
    when(analytics.weekly(anyString(), any(), any()))
        .thenReturn(new AdherenceContextFacade.WeeklyMetrics(0, 0));
    when(intakes.history(anyString(), any(), any())).thenReturn(List.of());
    when(intakes.next(anyString())).thenReturn(Optional.empty());
  }

  private void sourceFailure(String suffix) throws Exception {
    mvc.perform(get("/api/v1/older-adults/{id}/" + suffix, owner).param("caregiverId", caregiver))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("MONITORING_SOURCE_ERROR"))
        .andExpect(jsonPath("$.message").value("Monitoring information could not be read"))
        .andExpect(jsonPath("$.cause").doesNotExist())
        .andExpect(jsonPath("$.stackTrace").doesNotExist())
        .andExpect(jsonPath("$.weeklyAdherence").doesNotExist());
  }

  @Test
  void internalSelectionFailureIsNotClassifiedAsCaregiverTransitionConflict() throws Exception {
    doThrow(new IllegalStateException("internal-selection-marker"))
        .when(selection)
        .requireCurrent(owner, caregiver);
    mvc.perform(
            get("/api/v1/older-adults/{id}/status", owner)
                .param("caregiverId", caregiver)
                .header("Accept-Language", "en"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("MONITORING_STATE_ERROR"))
        .andExpect(
            jsonPath("$.message")
                .value("Follow-up information is inconsistent and could not be processed"));
  }

  @Test
  void impossibleAnalyticsCountsFailAsServerEvidenceAndRecoverWithHealthySource() throws Exception {
    // A deliberately corrupt provider bypasses the owning facade's constructor invariant.
    var invalid = mock(AdherenceContextFacade.WeeklyMetrics.class);
    when(invalid.confirmedIntakes()).thenReturn(5);
    when(invalid.totalIntakes()).thenReturn(4);
    when(analytics.weekly(anyString(), any(), any())).thenReturn(invalid);
    sourceFailure("status");
    when(analytics.weekly(anyString(), any(), any()))
        .thenReturn(new AdherenceContextFacade.WeeklyMetrics(3, 4));
    mvc.perform(get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weeklyAdherence.confirmedIntakes").value(3));
  }

  @Test
  void sourceValidationDetailsAreNotReturnedAsClientErrors() throws Exception {
    when(analytics.weekly(anyString(), any(), any()))
        .thenThrow(new IllegalArgumentException("source-detail-marker"));
    sourceFailure("status");
  }

  @Test
  void malformedHistoryAndPresentNextIntakeWithoutScheduleAreNotReportedAsNoData()
      throws Exception {
    var invalid =
        new IntakeResult(
            "intake",
            "treatment",
            "medication",
            owner,
            "Name",
            "Dose",
            "",
            null,
            IntakeStatus.CONFIRMED,
            null,
            null,
            false);
    when(intakes.history(anyString(), any(), any())).thenReturn(List.of(invalid));
    sourceFailure("intakes");
    when(intakes.history(anyString(), any(), any())).thenReturn(List.of());
    when(intakes.next(anyString())).thenReturn(Optional.of(invalid));
    sourceFailure("status");
  }

  @Test
  void evidenceForAnotherAdultIsNeverExposedThroughHistoryOrNextIntake() throws Exception {
    var foreign =
        new IntakeResult(
            "intake",
            "treatment",
            "medication",
            UUID.randomUUID().toString(),
            "Foreign medication",
            "Dose",
            "",
            clock.instant(),
            IntakeStatus.CONFIRMED,
            null,
            null,
            false);
    when(intakes.history(anyString(), any(), any())).thenReturn(List.of(foreign));
    sourceFailure("intakes");
    when(intakes.history(anyString(), any(), any())).thenReturn(List.of());
    when(intakes.next(anyString())).thenReturn(Optional.of(foreign));
    sourceFailure("status");
  }

  @Test
  void sourceErrorCodeIsStableAcrossRequestedLanguages() throws Exception {
    // A deliberately corrupt provider bypasses the owning facade's constructor invariant.
    var invalid = mock(AdherenceContextFacade.WeeklyMetrics.class);
    when(invalid.confirmedIntakes()).thenReturn(5);
    when(invalid.totalIntakes()).thenReturn(4);
    when(analytics.weekly(anyString(), any(), any())).thenReturn(invalid);
    mvc.perform(
            get("/api/v1/older-adults/{id}/status", owner)
                .param("caregiverId", caregiver)
                .header("Accept-Language", "es-PE"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("MONITORING_SOURCE_ERROR"))
        .andExpect(
            jsonPath("$.message").value("No se pudo consultar la información de seguimiento"));
    mvc.perform(
            get("/api/v1/older-adults/{id}/status", owner)
                .param("caregiverId", caregiver)
                .header("Accept-Language", "en"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("MONITORING_SOURCE_ERROR"))
        .andExpect(jsonPath("$.message").value("Monitoring information could not be read"));
  }

  @Test
  void validEmptyEvidenceAndClientValidationRetainTheirDistinctContracts() throws Exception {
    mvc.perform(get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weeklyAdherence.totalIntakes").value(0))
        .andExpect(jsonPath("$.openAlerts.length()").value(0));
    mvc.perform(
            get("/api/v1/older-adults/{id}/contact-channel", owner).param("caregiverId", caregiver))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/v1/older-adults/{id}/intakes", owner)
                .param("caregiverId", caregiver)
                .param("days", "0"))
        .andExpect(status().isBadRequest());
  }
}
