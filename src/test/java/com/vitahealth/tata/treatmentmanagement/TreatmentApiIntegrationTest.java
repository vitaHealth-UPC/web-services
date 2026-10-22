package com.vitahealth.tata.treatmentmanagement;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.treatmentmanagement.application.internal.TreatmentSchedulePublicationService;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Exercises treatment lifecycle through authenticated HTTP with real consented care links. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TreatmentApiIntegrationTest {
  private static final String CAREGIVER = "caregiver-1";

  @Autowired WebApplicationContext context;
  @Autowired jakarta.persistence.EntityManager entityManager;
  @Autowired AccountRepository accounts;
  @Autowired SessionTokenService tokens;
  @Autowired CareLinkCommandService links;
  @Autowired IntakeRepository intakes;
  @Autowired TreatmentRepository treatments;

  @Autowired
  com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository medications;

  @Autowired com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade careLinks;
  @Autowired TreatmentSchedulePublicationService publication;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    accounts.save(
        Account.rehydrate(
            CAREGIVER,
            "Caregiver",
            new EmailAddress("treatment@example.com"),
            "hash",
            AccountStatus.ACTIVE,
            null,
            null));
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(
                get("/").header("Authorization", "Bearer " + tokens.issue(CAREGIVER).accessToken()))
            .build();
  }

  private String newAdult() {
    var profile =
        links
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    CAREGIVER, "Rosa", LocalDate.of(1950, 1, 1), "Diego", "Hijo", "+51999999999"))
            .toOptional()
            .orElseThrow();
    var generated =
        links
            .generateLinkingCode(new GenerateLinkingCodeCommand(CAREGIVER, profile.id()))
            .toOptional()
            .orElseThrow();
    var accepted =
        links
            .accept(new AcceptCareLinkCommand(CAREGIVER, generated.linkingCode()))
            .toOptional()
            .orElseThrow();
    links
        .registerConsent(new RegisterConsentCommand(accepted.id(), true))
        .toOptional()
        .orElseThrow();
    return profile.id();
  }

  private String json(String path, String body) throws Exception {
    var response =
        mockMvc
            .perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(response, "$.id");
  }

  private String registerMedication(String adult, String name) throws Exception {
    return json(
        "/api/v1/older-adults/" + adult + "/medications",
        "{\"caregiverId\":\""
            + CAREGIVER
            + "\",\"name\":\""
            + name
            + "\",\"presentation\":\"50 mg\"}");
  }

  private String createTreatment(String adult, String name) throws Exception {
    return json(
        "/api/v1/older-adults/" + adult + "/treatments",
        "{\"caregiverId\":\"" + CAREGIVER + "\",\"name\":\"" + name + "\"}");
  }

  private void configure(String treatmentId, String medicationId) throws Exception {
    mockMvc
        .perform(
            put("/api/v1/treatments/" + treatmentId + "/regimen")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"caregiverId\":\""
                        + CAREGIVER
                        + "\",\"medicationId\":\""
                        + medicationId
                        + "\",\"dose\":\"1"
                        + " comprimido\",\"frequency\":\"DAILY\",\"scheduledTimes\":[\"08:00:00\",\"20:00:00\"],\"instructions\":\"Con"
                        + " agua\",\"reminderLeadMinutes\":10}"))
        .andExpect(status().isOk());
  }

  @Test
  void followUpDelaySurvivesStorageAndDoesNotResetAnExplicitRecurrence() throws Exception {
    var adult = newAdult();
    var medication = registerMedication(adult, "Losartan");
    var treatment = createTreatment(adult, "Control");
    var base =
        "{\"caregiverId\":\""
            + CAREGIVER
            + "\",\"medicationId\":\""
            + medication
            + "\",\"dose\":\"1 tableta\",\"frequency\":\"cada 12"
            + " horas\",\"scheduledTimes\":[\"08:00:00\",\"20:00:00\"],\"instructions\":\"Con"
            + " agua\",\"reminderLeadMinutes\":7";
    mockMvc
        .perform(
            put("/api/v1/treatments/" + treatment + "/regimen")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    base
                        + ",\"followUpDelayMinutes\":25,\"recurrenceMode\":\"FIXED_INTERVAL\",\"recurrenceZone\":\"America/Bogota\",\"recurrenceAnchor\":\"2026-10-21T13:00:00Z\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.followUpDelayMinutes").value(25));
    entityManager.flush();
    entityManager.clear();
    mockMvc
        .perform(get("/api/v1/treatments/" + treatment).param("caregiverId", CAREGIVER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.followUpDelayMinutes").value(25));
    for (String value : List.of("0", "null")) {
      mockMvc
          .perform(
              put("/api/v1/treatments/" + treatment + "/regimen")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(base + ",\"followUpDelayMinutes\":" + value + "}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.recurrenceMode").value("FIXED_INTERVAL"))
          .andExpect(jsonPath("$.recurrenceZone").value("America/Bogota"))
          .andExpect(jsonPath("$.reminderLeadMinutes").value(7));
      entityManager.flush();
      entityManager.clear();
      assertEquals(
          value.equals("null") ? null : Integer.valueOf(0),
          treatments.findById(treatment).orElseThrow().regimen().followUpDelayMinutes());
    }
    mockMvc
        .perform(
            put("/api/v1/treatments/" + treatment + "/regimen")
                .contentType(MediaType.APPLICATION_JSON)
                .content(base + ",\"followUpDelayMinutes\":-1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void explicitIntervalConfiguredByHttpGeneratesAnchoredTakesAndReplaysWithoutDuplicates()
      throws Exception {
    var adult = newAdult();
    var medication = registerMedication(adult, "Losartán");
    var treatment = createTreatment(adult, "Presión");
    var anchor = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.MICROS);
    var request =
        """
        {"caregiverId":"%s","medicationId":"%s","dose":"1 comprimido",
         "frequency":"Cada 12 Horas","scheduledTimes":["08:00:00","20:00:00"],
         "instructions":"","reminderLeadMinutes":10,"recurrenceMode":"FIXED_INTERVAL",
         "recurrenceZone":"America/Bogota","recurrenceAnchor":"%s"}
        """
            .formatted(CAREGIVER, medication, anchor);
    mockMvc
        .perform(
            put("/api/v1/treatments/" + treatment + "/regimen")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recurrenceMode").value("FIXED_INTERVAL"))
        .andExpect(jsonPath("$.recurrenceZone").value("America/Bogota"))
        .andExpect(jsonPath("$.recurrenceAnchor").value(anchor.toString()));
    mockMvc
        .perform(
            post("/api/v1/treatments/" + treatment + "/activation").param("caregiverId", CAREGIVER))
        .andExpect(status().isOk());
    var before = intakes.findFutureByTreatmentId(treatment, anchor);
    assertTrue(before.size() >= 10);
    assertEquals(anchor, before.getFirst().scheduledAt());
    for (int i = 1; i < before.size(); i++)
      assertEquals(
          Duration.ofHours(12),
          Duration.between(before.get(i - 1).scheduledAt(), before.get(i).scheduledAt()));
    // An older Android client omits recurrence fields while editing an existing regimen.
    var olderClientRequest =
        """
        {"caregiverId":"%s","medicationId":"%s","dose":"1 comprimido",
         "frequency":"Cada 12 Horas","scheduledTimes":["08:00:00","20:00:00"],
         "instructions":"Con agua","reminderLeadMinutes":10}
        """
            .formatted(CAREGIVER, medication);
    mockMvc
        .perform(
            put("/api/v1/treatments/" + treatment + "/regimen")
                .contentType(MediaType.APPLICATION_JSON)
                .content(olderClientRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recurrenceMode").value("FIXED_INTERVAL"))
        .andExpect(jsonPath("$.recurrenceZone").value("America/Bogota"))
        .andExpect(jsonPath("$.recurrenceAnchor").value(anchor.toString()));
    publication.publish(treatments.findById(treatment).orElseThrow());
    var after = intakes.findFutureByTreatmentId(treatment, anchor);
    assertEquals(
        before.stream().map(value -> value.id()).toList(),
        after.stream().map(value -> value.id()).toList());
  }

  @Test
  void malformedRecurrenceAndJsonPreserveTheStoredRegimen() throws Exception {
    var adult = newAdult();
    var medication = registerMedication(adult, "Losartán");
    var treatment = createTreatment(adult, "Presión");
    configure(treatment, medication);
    var before = treatments.findById(treatment).orElseThrow().regimen();
    var prefix =
        """
        {"caregiverId":"%s","medicationId":"%s","dose":"changed",
         "frequency":"cada 12 horas","scheduledTimes":["08:00:00","20:00:00"],
         "instructions":"","reminderLeadMinutes":10,
        """
            .formatted(CAREGIVER, medication);
    for (var metadata :
        List.of(
            "\"recurrenceZone\":\"America/Bogota\"}",
            "\"recurrenceMode\":\"UNKNOWN\",\"recurrenceZone\":\"UTC\"}",
            "\"recurrenceMode\":\"FIXED_INTERVAL\",\"recurrenceZone\":\"UTC\"}",
            "\"recurrenceMode\":\"LOCAL_TIMES\",\"recurrenceZone\":\"not-a-zone\"}",
            "\"recurrenceMode\":\"FIXED_INTERVAL\",\"recurrenceZone\":\"UTC\",\"recurrenceAnchor\":\"not-an-instant\"}",
            "\"recurrenceMode\":")) {
      mockMvc
          .perform(
              put("/api/v1/treatments/" + treatment + "/regimen")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(prefix + metadata))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.detail").value("request validation failed"));
      assertEquals(before, treatments.findById(treatment).orElseThrow().regimen());
    }
  }

  @Test
  void medicationsAreListedByOlderAdult() throws Exception {
    var adult = newAdult();
    registerMedication(adult, "Losartán");
    registerMedication(adult, "Atorvastatina");
    registerMedication(newAdult(), "Metformina");

    mockMvc
        .perform(
            get("/api/v1/older-adults/" + adult + "/medications").param("caregiverId", CAREGIVER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name").value("Atorvastatina"))
        .andExpect(jsonPath("$[1].name").value("Losartán"));
  }

  @Test
  void anIncompleteTreatmentCannotBeActivated() throws Exception {
    var treatmentId = createTreatment(newAdult(), "Presión");

    mockMvc
        .perform(
            post("/api/v1/treatments/" + treatmentId + "/activation")
                .param("caregiverId", CAREGIVER))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("INCOMPLETE_TREATMENT"));
  }

  @Test
  void aTreatmentGoesFromDraftToActiveToPausedAndBack() throws Exception {
    var adult = newAdult();
    var medicationId = registerMedication(adult, "Losartán");
    var treatmentId = createTreatment(adult, "Presión");
    configure(treatmentId, medicationId);

    mockMvc
        .perform(
            post("/api/v1/treatments/" + treatmentId + "/activation")
                .param("caregiverId", CAREGIVER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    mockMvc
        .perform(
            post("/api/v1/treatments/" + treatmentId + "/pause").param("caregiverId", CAREGIVER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAUSED"))
        .andExpect(jsonPath("$.scheduledTimes", hasSize(2)));
    mockMvc
        .perform(
            post("/api/v1/treatments/" + treatmentId + "/resume").param("caregiverId", CAREGIVER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc
        .perform(
            get("/api/v1/older-adults/" + adult + "/treatments").param("caregiverId", CAREGIVER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].medicationId").value(medicationId));
  }

  @Test
  void deactivatingTheMedicationPausesItsActiveTreatmentAndBlocksResuming() throws Exception {
    var adult = newAdult();
    var medicationId = registerMedication(adult, "Losartán");
    var treatmentId = createTreatment(adult, "Presión");
    configure(treatmentId, medicationId);
    mockMvc
        .perform(
            post("/api/v1/treatments/" + treatmentId + "/activation")
                .param("caregiverId", CAREGIVER))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/medications/" + medicationId + "/deactivation")
                .param("caregiverId", CAREGIVER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mockMvc
        .perform(get("/api/v1/treatments/" + treatmentId).param("caregiverId", CAREGIVER))
        .andExpect(jsonPath("$.status").value("PAUSED"));
    mockMvc
        .perform(
            post("/api/v1/treatments/" + treatmentId + "/resume").param("caregiverId", CAREGIVER))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("MEDICATION_INACTIVE"));
    var before = intakes.findFutureByTreatmentId(treatmentId, Instant.EPOCH).size();
    for (int retry = 0; retry < 2; retry++) {
      mockMvc
          .perform(
              post("/api/v1/medications/" + medicationId + "/reactivation")
                  .param("caregiverId", CAREGIVER))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.active").value(true));
      mockMvc
          .perform(get("/api/v1/treatments/" + treatmentId).param("caregiverId", CAREGIVER))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("PAUSED"));
    }
    assertEquals(before, intakes.findFutureByTreatmentId(treatmentId, Instant.EPOCH).size());
    mockMvc
        .perform(
            post("/api/v1/treatments/" + treatmentId + "/resume").param("caregiverId", CAREGIVER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void medicationReactivationRejectsAnonymousForeignAndWithdrawnAccessWithoutChangingAvailability()
      throws Exception {
    var adult = newAdult();
    var medication = registerMedication(adult, "Losartan");
    mockMvc
        .perform(
            post("/api/v1/medications/" + medication + "/deactivation")
                .param("caregiverId", CAREGIVER))
        .andExpect(status().isOk());
    var path = "/api/v1/medications/" + medication + "/reactivation";
    var anonymous = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    anonymous
        .perform(post(path).param("caregiverId", CAREGIVER))
        .andExpect(status().isUnauthorized());
    var stranger = UUID.randomUUID().toString();
    accounts.save(
        Account.rehydrate(
            stranger,
            "Other",
            new EmailAddress(stranger + "@example.com"),
            "hash",
            AccountStatus.ACTIVE,
            null,
            null));
    anonymous
        .perform(
            post(path)
                .param("caregiverId", stranger)
                .header("Authorization", "Bearer " + tokens.issue(stranger).accessToken()))
        .andExpect(status().isForbidden());
    assertTrue(!medications.findById(medication).orElseThrow().active());
    var link =
        careLinks.getConfirmedByCaregiver(CAREGIVER).stream()
            .filter(value -> value.olderAdultId().equals(adult))
            .findFirst()
            .orElseThrow();
    links
        .withdrawConsent(
            new com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand(
                link.id(), adult))
        .toOptional()
        .orElseThrow();
    mockMvc.perform(post(path).param("caregiverId", CAREGIVER)).andExpect(status().isForbidden());
    assertTrue(!medications.findById(medication).orElseThrow().active());
  }

  @Test
  void aCaregiverWithoutCareLinkIsRejectedEverywhere() throws Exception {
    var adult = newAdult();

    mockMvc
        .perform(
            post("/api/v1/older-adults/" + adult + "/medications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"caregiverId\":\"intruder\",\"name\":\"Losartán\",\"presentation\":\"50"
                        + " mg\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("RESOURCE_ACCESS_DENIED"));
    mockMvc
        .perform(
            get("/api/v1/older-adults/" + adult + "/treatments").param("caregiverId", "intruder"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/api/v1/older-adults/" + adult + "/medications").param("caregiverId", "intruder"))
        .andExpect(status().isForbidden());
  }

  @Test
  void missingFieldsAreRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/older-adults/" + newAdult() + "/medications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"caregiverId\":\"" + CAREGIVER + "\",\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
  }

  @Test
  void unknownTreatmentDoesNotDisclosePrivateResourceExistence() throws Exception {
    mockMvc
        .perform(get("/api/v1/treatments/" + UUID.randomUUID()).param("caregiverId", CAREGIVER))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("RESOURCE_ACCESS_DENIED"));
  }
}
