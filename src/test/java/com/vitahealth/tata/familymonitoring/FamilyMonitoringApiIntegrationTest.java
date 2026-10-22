package com.vitahealth.tata.familymonitoring;

import static com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus.ATTENDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.vitahealth.tata.adherenceanalytics.domain.model.events.AdherencePatternDetected;
import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.events.CareLinkConfirmed;
import com.vitahealth.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.FamilyMonitorSelectionService;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.*;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.inventoryreplenishment.domain.model.events.InventoryStockChanged;
import com.vitahealth.tata.omissionescalation.domain.model.events.IntakeOmitted;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FamilyMonitoringApiIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired CareLinkRepository links;
  @Autowired AccountRepository accounts;
  @Autowired SessionTokenService tokens;

  @Autowired CareLinkCommandService linkCommands;
  @Autowired Clock clock;

  @Autowired IFamilyMonitorRepository monitors;
  @Autowired FamilyMonitorSelectionService selection;
  @Autowired IntakeRepository intakes;
  @Autowired OlderAdultProfileRepository profiles;
  @Autowired MedicationRepository medications;

  @Autowired ApplicationEventPublisher events;

  MockMvc mvc;
  String owner, caregiver;

  @BeforeEach
  void setup() {
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
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(
                get("/").header("Authorization", "Bearer " + tokens.issue(caregiver).accessToken()))
            .build();
    var now = clock.instant();
    var code = UUID.randomUUID().toString().replace("-", "");
    var link =
        CareLink.createPending(
            caregiver, owner, LinkingCode.issue(code, now, Duration.ofMinutes(15)), now);
    link.accept(code, now);
    links.save(link);
    linkCommands
        .registerConsent(new RegisterConsentCommand(link.id(), true))
        .toOptional()
        .orElseThrow();
  }

  private Intake dose(Instant scheduled) {
    return Intake.createScheduled(
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        owner,
        new MedicationSnapshot("Medication", "1 tablet", ""),
        scheduled,
        Instant.now());
  }

  @Test
  void summaryAndHistoryUseDefinitiveOutcomesAndFuturePendingDose() throws Exception {
    var now = clock.instant();
    var confirmed = dose(now.minusSeconds(300));
    confirmed.confirm(confirmed.scheduledAt(), ConfirmationChannel.TOUCH);
    var omitted = dose(now.minusSeconds(200));
    omitted.markOmitted();
    var future = dose(now.plusSeconds(3600));
    intakes.saveAll(List.of(confirmed, omitted, dose(now.minusSeconds(100)), future));
    mvc.perform(get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weeklyAdherence.confirmedIntakes").value(1))
        .andExpect(jsonPath("$.weeklyAdherence.totalIntakes").value(2))
        .andExpect(jsonPath("$.lastIntakeStatus").value("OMITTED"))
        .andExpect(jsonPath("$.nextIntakeAt").exists());
    mvc.perform(get("/api/v1/older-adults/{id}/intakes", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void closedAlertRejectsFurtherTransitionsAsBusinessConflictAndRetainsHistory() throws Exception {
    var monitor = selection.requireCurrent(owner, caregiver);
    var now = clock.instant();
    monitor.addAlert(
        UUID.randomUUID().toString(), "Medication", now.minusSeconds(60), "Omitted", now);
    var alertId = monitors.save(monitor).getAlerts().getFirst().getId();
    mvc.perform(
            put("/api/v1/older-adults/{id}/alerts/{alert}/status", owner, alertId)
                .param("caregiverId", caregiver)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}"))
        .andExpect(status().isOk());
    for (String transition : List.of("ATTENDED", "CLOSED"))
      mvc.perform(
              put("/api/v1/older-adults/{id}/alerts/{alert}/status", owner, alertId)
                  .param("caregiverId", caregiver)
                  .header("Accept-Language", "es-PE")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"status\":\"" + transition + "\"}"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("INVALID_STATE"))
          .andExpect(
              jsonPath("$.message").value("El cambio de estado solicitado no está disponible"));
    mvc.perform(
            get("/api/v1/older-adults/{id}/alerts/{alert}", owner, alertId)
                .param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  void unrelatedCaregiverCannotReadAnyMonitoringResourceOrWriteNotes() throws Exception {
    var stranger = UUID.randomUUID().toString();
    for (var suffix : List.of("status", "intakes", "contact-channel", "notes", "alerts/1"))
      mvc.perform(
              get("/api/v1/older-adults/" + owner + "/" + suffix).param("caregiverId", stranger))
          .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/older-adults/{id}/notes", owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"familiarId\":\"" + stranger + "\",\"text\":\"Follow-up\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/v1/older-adults/{id}/alerts/1/status", owner)
                .param("caregiverId", stranger)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ATTENDED\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void notesRetainAuthorAndTimeAndEmptySummaryHasZeroEvidence() throws Exception {
    mvc.perform(
            post("/api/v1/older-adults/{id}/notes", owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"familiarId\":\"" + caregiver + "\",\"text\":\"Called caregiver\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.familiarId").value(caregiver));
    mvc.perform(get("/api/v1/older-adults/{id}/notes", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].recordedAt").exists());
    mvc.perform(get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weeklyAdherence.totalIntakes").value(0));
  }

  @Test
  void creatingANoteReturnsItsOwnIdentityEvenWhenAnExistingNoteHasALaterDate() throws Exception {
    var followUp = selection.requireCurrent(owner, caregiver);
    followUp.addNote("Existing later-dated note", caregiver, clock.instant().plusSeconds(3600));
    var previousId = monitors.save(followUp).getNotes().getFirst().getId();

    var response =
        mvc.perform(
                post("/api/v1/older-adults/{id}/notes", owner)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"familiarId\":\"" + caregiver + "\",\"text\":\"Current intervention\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.text").value("Current intervention"))
            .andReturn();
    Number createdId = JsonPath.read(response.getResponse().getContentAsString(), "$.id");
    assertNotEquals(previousId.longValue(), createdId.longValue());
    var saved = selection.requireCurrent(owner, caregiver);
    assertEquals(2, saved.getNotes().size());
    assertEquals(
        true,
        saved.getNotes().stream()
            .anyMatch(
                note ->
                    note.getId().equals(createdId.longValue())
                        && note.getText().equals("Current intervention")));
    assertEquals(
        true,
        saved.getNotes().stream()
            .anyMatch(
                note ->
                    note.getId().equals(previousId)
                        && note.getText().equals("Existing later-dated note")));
  }

  @Test
  void replacingAWithdrawnLinkSelectsNewFollowUpAndRetainsOldPrivateHistory() throws Exception {
    var old = selection.requireCurrent(owner, caregiver);
    var now = clock.instant();
    old.addNote("Old relationship note", caregiver, now);
    old.addAlert(UUID.randomUUID().toString(), "Old relationship medication", now, "omission", now);
    old = monitors.save(old);
    var oldAlertId = old.getAlerts().getFirst().getId();
    var oldId = old.getId();
    var oldLinkId = old.getCareLinkId();
    var withdrawn = links.findById(oldLinkId).orElseThrow();
    withdrawn.withdrawConsent(owner, clock.instant());
    links.save(withdrawn);
    var replacement = confirmedFollowUp(caregiver, clock.instant());

    mvc.perform(get("/api/v1/older-adults/{id}/notes", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openAlerts.length()").value(0));
    mvc.perform(
            get("/api/v1/older-adults/{id}/alerts/{alert}", owner, oldAlertId)
                .param("caregiverId", caregiver))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/v1/older-adults/{id}/alerts/{alert}/status", owner, oldAlertId)
                .param("caregiverId", caregiver)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/older-adults/{id}/notes", owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"familiarId\":\"" + caregiver + "\",\"text\":\"New relationship note\"}"))
        .andExpect(status().isCreated());

    var preserved = monitors.findByCareLinkId(oldLinkId).orElseThrow();
    assertEquals(oldId, preserved.getId());
    assertEquals("Old relationship note", preserved.getNotes().getFirst().getText());
    assertEquals(1, preserved.getNotes().size());
    assertEquals(oldAlertId, preserved.getAlerts().getFirst().getId());
    assertEquals(
        "New relationship note",
        monitors.findByCareLinkId(replacement.id()).orElseThrow().getNotes().getFirst().getText());
  }

  @Test
  void adherencePatternsRequireMedicationOwnershipAndCurrentRelationshipConsent() {
    var now = clock.instant();
    var second = confirmedFollowUp(UUID.randomUUID().toString(), now);
    var withdrawn = confirmedFollowUp(UUID.randomUUID().toString(), now);
    withdrawn.withdrawConsent(owner, clock.instant());
    links.save(withdrawn);
    var medication = medications.save(Medication.register(owner, "Losartan", "50 mg", now));
    var first = LocalDate.parse("2026-10-01");
    var last = first.plusDays(2);
    var valid = new AdherencePatternDetected(owner, medication.id(), 3, first, last, now);
    events.publishEvent(valid);
    events.publishEvent(valid);
    var foreignMedication =
        medications.save(
            Medication.register(
                UUID.randomUUID().toString(), "Other adult medication", "50 mg", now));
    events.publishEvent(
        new AdherencePatternDetected(owner, foreignMedication.id(), 3, first, last, now));
    events.publishEvent(
        new AdherencePatternDetected(owner, UUID.randomUUID().toString(), 3, first, last, now));

    for (String participant : List.of(caregiver, second.caregiverId())) {
      var insights = selection.requireCurrent(owner, participant).getAdherenceInsights();
      assertEquals(1, insights.size());
      assertEquals(medication.id(), insights.getFirst().getMedicationId());
      assertEquals("Losartan", insights.getFirst().getMedicationName());
      assertEquals(now, insights.getFirst().getDetectedAt());
    }
    assertEquals(
        0, monitors.findByCareLinkId(withdrawn.id()).orElseThrow().getAdherenceInsights().size());
  }

  @Test
  void stockProjectionAndReplenishmentUseEveryConsentedFollowUp() {
    var now = clock.instant();
    var second = confirmedFollowUp(UUID.randomUUID().toString(), now);
    var withdrawn = confirmedFollowUp(UUID.randomUUID().toString(), now);
    withdrawn.withdrawConsent(owner, clock.instant());
    links.save(withdrawn);
    var medication = medications.save(Medication.register(owner, "Losartan", "50 mg", now));
    var low =
        new InventoryStockChanged(UUID.randomUUID().toString(), medication.id(), 3, 5, 0, now);

    events.publishEvent(low);
    events.publishEvent(low);
    for (String participant : List.of(caregiver, second.caregiverId())) {
      var notices = selection.requireCurrent(owner, participant).getLowStockNotices();
      assertEquals(1, notices.size());
      assertEquals(3, notices.getFirst().getRemainingStock());
      assertEquals(now, notices.getFirst().getDetectedAt());
    }
    assertEquals(
        0, monitors.findByCareLinkId(withdrawn.id()).orElseThrow().getLowStockNotices().size());
    events.publishEvent(
        new InventoryStockChanged(low.inventoryId(), medication.id(), 33, 5, 1, clock.instant()));
    events.publishEvent(low);
    for (String participant : List.of(caregiver, second.caregiverId()))
      assertEquals(0, selection.requireCurrent(owner, participant).getLowStockNotices().size());
  }

  @Test
  void omissionTraversesMoreThanOneHundredFollowUpsAndKeepsRevokedHistoryUntouched() {
    var now = clock.instant();
    var followUps = new ArrayList<CareLink>();
    for (int index = 0; index < 104; index++)
      followUps.add(confirmedFollowUp(UUID.randomUUID().toString(), now));
    var revoked = followUps.removeLast();
    revoked.withdrawConsent(owner, clock.instant());
    links.save(revoked);
    var replacement = confirmedFollowUp(revoked.caregiverId(), clock.instant());
    followUps.add(replacement);
    var firstPage = monitors.findIdsByOlderAdultIdAfter(owner, 0, 100);
    var lastPage = monitors.findIdsByOlderAdultIdAfter(owner, firstPage.getLast(), 100);
    assertEquals(100, firstPage.size());
    assertEquals(6, lastPage.size());
    assertEquals(true, lastPage.getFirst() > firstPage.getLast());
    var occurrence = now.plusSeconds(1);
    var event =
        new IntakeOmitted(
            UUID.randomUUID().toString(),
            owner,
            "Medication",
            now.minusSeconds(1800),
            "omission",
            occurrence);

    events.publishEvent(event);
    events.publishEvent(event);

    for (CareLink relationship : followUps) {
      var alerts = monitors.findByCareLinkId(relationship.id()).orElseThrow().getAlerts();
      assertEquals(1, alerts.size());
      assertEquals(occurrence, alerts.getFirst().getOpenedAt());
    }
    assertEquals(1, selection.requireCurrent(owner, caregiver).getAlerts().size());
    assertEquals(0, monitors.findByCareLinkId(revoked.id()).orElseThrow().getAlerts().size());
  }

  @Test
  void omissionReachesBothConsentedFollowUpsButSkipsTheWithdrawnRelationship() {
    var now = clock.instant();
    var second = confirmedFollowUp(UUID.randomUUID().toString(), now);
    var withdrawn = confirmedFollowUp(UUID.randomUUID().toString(), now);
    withdrawn.withdrawConsent(owner, clock.instant());
    links.save(withdrawn);
    var occurredAt = now.minusSeconds(30);
    var event =
        new IntakeOmitted(
            UUID.randomUUID().toString(),
            owner,
            "Medication",
            occurredAt.minusSeconds(1800),
            "omission",
            occurredAt);

    events.publishEvent(event);
    events.publishEvent(event);

    for (String participant : List.of(caregiver, second.caregiverId())) {
      var alerts = selection.requireCurrent(owner, participant).getAlerts();
      assertEquals(1, alerts.size());
      assertEquals(event.intakeId(), alerts.getFirst().getIntakeId());
      assertEquals(occurredAt, alerts.getFirst().getOpenedAt());
    }
    assertEquals(0, monitors.findByCareLinkId(withdrawn.id()).orElseThrow().getAlerts().size());
  }

  private CareLink confirmedFollowUp(String participant, Instant now) {
    var code = UUID.randomUUID().toString().replace("-", "");
    var link =
        CareLink.createPending(
            participant, owner, LinkingCode.issue(code, now, Duration.ofMinutes(15)), now);
    link.accept(code, now);
    links.save(link);
    linkCommands
        .registerConsent(new RegisterConsentCommand(link.id(), true))
        .toOptional()
        .orElseThrow();
    return links.findById(link.id()).orElseThrow();
  }

  @Test
  void notesUseTheAuthenticatedCaregiversFollowUpWhenTheAdultHasTwoCaregivers() throws Exception {
    var other = UUID.randomUUID().toString();
    accounts.save(
        Account.rehydrate(
            other,
            "Second caregiver",
            new EmailAddress(other + "@example.com"),
            "hash",
            AccountStatus.ACTIVE,
            null,
            null));
    var now = clock.instant();
    var code = UUID.randomUUID().toString().replace("-", "");
    var link =
        CareLink.createPending(
            other, owner, LinkingCode.issue(code, now, Duration.ofMinutes(15)), now);
    link.accept(code, now);
    links.save(link);
    linkCommands
        .registerConsent(new RegisterConsentCommand(link.id(), true))
        .toOptional()
        .orElseThrow();
    var otherToken = tokens.issue(other).accessToken();

    mvc.perform(
            post("/api/v1/older-adults/{id}/notes", owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"familiarId\":\"" + caregiver + "\",\"text\":\"First intervention\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/older-adults/{id}/notes", owner)
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"familiarId\":\"" + other + "\",\"text\":\"Second intervention\"}"))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/v1/older-adults/{id}/notes", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].text").value("First intervention"));
    mvc.perform(
            get("/api/v1/older-adults/{id}/notes", owner)
                .header("Authorization", "Bearer " + otherToken)
                .param("caregiverId", other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].text").value("Second intervention"));
    mvc.perform(get("/api/v1/older-adults/{id}/notes", owner).param("caregiverId", other))
        .andExpect(status().isForbidden());

    var firstFollowUp = selection.requireCurrent(owner, caregiver);
    firstFollowUp.addAlert(UUID.randomUUID().toString(), "First medication", now, "omission", now);
    var firstAlert = monitors.save(firstFollowUp).getAlerts().getFirst().getId();
    var secondFollowUp = selection.requireCurrent(owner, other);
    secondFollowUp.addAlert(
        UUID.randomUUID().toString(), "Second medication", now, "omission", now);
    var secondAlert = monitors.save(secondFollowUp).getAlerts().getFirst().getId();

    mvc.perform(get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openAlerts.length()").value(1))
        .andExpect(jsonPath("$.openAlerts[0].medicationName").value("First medication"));
    mvc.perform(
            get("/api/v1/older-adults/{id}/status", owner)
                .header("Authorization", "Bearer " + otherToken)
                .param("caregiverId", other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openAlerts.length()").value(1))
        .andExpect(jsonPath("$.openAlerts[0].medicationName").value("Second medication"));
    mvc.perform(get("/api/v1/older-adults/{id}/intakes", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            get("/api/v1/older-adults/{id}/intakes", owner)
                .header("Authorization", "Bearer " + otherToken)
                .param("caregiverId", other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            get("/api/v1/older-adults/{id}/contact-channel", owner).param("caregiverId", caregiver))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/v1/older-adults/{id}/contact-channel", owner)
                .header("Authorization", "Bearer " + otherToken)
                .param("caregiverId", other))
        .andExpect(status().isNotFound());

    mvc.perform(
            get("/api/v1/older-adults/{id}/alerts/{alert}", owner, firstAlert)
                .param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.medicationName").value("First medication"));
    mvc.perform(
            get("/api/v1/older-adults/{id}/alerts/{alert}", owner, secondAlert)
                .param("caregiverId", caregiver))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/v1/older-adults/{id}/alerts/{alert}/status", owner, secondAlert)
                .param("caregiverId", caregiver)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ATTENDED\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/v1/older-adults/{id}/alerts/{alert}/status", owner, firstAlert)
                .param("caregiverId", caregiver)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ATTENDED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ATTENDED"));
    mvc.perform(
            put("/api/v1/older-adults/{id}/alerts/{alert}/status", owner, secondAlert)
                .header("Authorization", "Bearer " + otherToken)
                .param("caregiverId", other)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
    assertEquals(
        ATTENDED, selection.requireCurrent(owner, caregiver).findAlert(firstAlert).getStatus());
    mvc.perform(get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasOpenAlert").value(true))
        .andExpect(jsonPath("$.openAlerts.length()").value(1))
        .andExpect(jsonPath("$.openAlerts[0].status").value("ATTENDED"));
    mvc.perform(
            get("/api/v1/older-adults/{id}/status", owner)
                .header("Authorization", "Bearer " + otherToken)
                .param("caregiverId", other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasOpenAlert").value(false))
        .andExpect(jsonPath("$.openAlerts.length()").value(0));
    mvc.perform(
            get("/api/v1/older-adults/{id}/alerts/{alert}", owner, secondAlert)
                .header("Authorization", "Bearer " + otherToken)
                .param("caregiverId", other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  void pendingRelationshipIsDeniedAndRepeatedConfirmationKeepsOneMonitor() throws Exception {
    var pendingCaregiver = UUID.randomUUID().toString();
    var now = clock.instant();
    links.save(
        CareLink.createPending(
            pendingCaregiver,
            owner,
            LinkingCode.issue(
                UUID.randomUUID().toString().replace("-", ""), now, Duration.ofMinutes(15)),
            now));
    mvc.perform(
            get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", pendingCaregiver))
        .andExpect(status().isForbidden());
    var monitor = selection.requireCurrent(owner, caregiver);
    events.publishEvent(new CareLinkConfirmed(monitor.getCareLinkId(), caregiver, owner));
    assertEquals(monitor.getId(), selection.requireCurrent(owner, caregiver).getId());
    mvc.perform(get("/api/v1/older-adults/{id}/status", owner)).andExpect(status().isBadRequest());
  }

  @Test
  void contactReadsRegisteredEmergencyPhoneAndMissingContactReturnsNotFound() throws Exception {
    mvc.perform(
            get("/api/v1/older-adults/{id}/contact-channel", owner).param("caregiverId", caregiver))
        .andExpect(status().isNotFound());
    var now = clock.instant();
    profiles.save(
        OlderAdultProfile.rehydrate(
            owner,
            caregiver,
            new OlderAdultBasicData("Older Adult", LocalDate.of(1950, 1, 1)),
            new EmergencyContact("Contact", "Family", "+51999999999"),
            now,
            now));
    mvc.perform(
            get("/api/v1/older-adults/{id}/contact-channel", owner).param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value("+51999999999"));
  }
}
