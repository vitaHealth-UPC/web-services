package com.tata.familymonitoring;

import com.tata.carelink.domain.model.aggregates.CareLink;
import com.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.tata.carelink.domain.repositories.CareLinkRepository;
import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.*;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.*;
import java.util.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FamilyMonitoringApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired CareLinkRepository links;
    @Autowired com.tata.carelink.application.commandservices.CareLinkCommandService linkCommands;
    @Autowired IFamilyMonitorRepository monitors;
    @Autowired IntakeRepository intakes;
    @Autowired com.tata.carelink.domain.repositories.OlderAdultProfileRepository profiles;
    @Autowired com.tata.familymonitoring.application.internal.eventhandlers.CareLinkConfirmedEventHandler followUp;
    MockMvc mvc;
    String owner, caregiver;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        owner = UUID.randomUUID().toString();
        caregiver = UUID.randomUUID().toString();
        var now = Instant.now();
        var code = UUID.randomUUID().toString().replace("-", "");
        var link = CareLink.createPending(caregiver, owner, LinkingCode.issue(code, now, Duration.ofMinutes(15)), now);
        link.accept(code, now);
        links.save(link);
        linkCommands.registerConsent(new com.tata.carelink.domain.model.commands.RegisterConsentCommand(link.id(), true));
    }
    private Intake dose(Instant scheduled) {
        return Intake.createScheduled(UUID.randomUUID().toString(), UUID.randomUUID().toString(), owner,
                new MedicationSnapshot("Medication", "1 tablet", ""), scheduled, Instant.now());
    }
    @Test void summaryAndHistoryUseDefinitiveOutcomesAndFuturePendingDose() throws Exception {
        var now = Instant.now();
        var confirmed = dose(now.minusSeconds(300));
        confirmed.confirm(confirmed.scheduledAt(), ConfirmationChannel.TOUCH);
        var omitted = dose(now.minusSeconds(200)); omitted.markOmitted();
        var future = dose(now.plusSeconds(3600));
        intakes.saveAll(List.of(confirmed, omitted, dose(now.minusSeconds(100)), future));
        mvc.perform(get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", caregiver))
                .andExpect(status().isOk()).andExpect(jsonPath("$.weeklyAdherence.confirmedIntakes").value(1))
                .andExpect(jsonPath("$.weeklyAdherence.totalIntakes").value(2))
                .andExpect(jsonPath("$.lastIntakeStatus").value("OMITTED"))
                .andExpect(jsonPath("$.nextIntakeAt").exists());
        mvc.perform(get("/api/v1/older-adults/{id}/intakes", owner).param("caregiverId", caregiver))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }
    @Test void unrelatedCaregiverCannotReadAnyMonitoringResourceOrWriteNotes() throws Exception {
        var stranger = UUID.randomUUID().toString();
        for (var suffix : List.of("status", "intakes", "contact-channel", "notes", "alerts/1"))
            mvc.perform(get("/api/v1/older-adults/" + owner + "/" + suffix).param("caregiverId", stranger))
                    .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/older-adults/{id}/notes", owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"familiarId\":\"" + stranger + "\",\"text\":\"Follow-up\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/older-adults/{id}/alerts/1/status", owner).param("caregiverId", stranger)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ATTENDED\"}"))
                .andExpect(status().isForbidden());
    }
    @Test void notesRetainAuthorAndTimeAndEmptySummaryHasZeroEvidence() throws Exception {
        mvc.perform(post("/api/v1/older-adults/{id}/notes", owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"familiarId\":\"" + caregiver + "\",\"text\":\"Called caregiver\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.familiarId").value(caregiver));
        mvc.perform(get("/api/v1/older-adults/{id}/notes", owner).param("caregiverId", caregiver))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recordedAt").exists());
        mvc.perform(get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", caregiver))
                .andExpect(status().isOk()).andExpect(jsonPath("$.weeklyAdherence.totalIntakes").value(0));
    }
    @Test void pendingRelationshipIsDeniedAndRepeatedConfirmationKeepsOneMonitor() throws Exception {
        var pendingCaregiver = UUID.randomUUID().toString();
        var now = Instant.now();
        links.save(CareLink.createPending(pendingCaregiver, owner,
                LinkingCode.issue(UUID.randomUUID().toString().replace("-", ""), now, Duration.ofMinutes(15)), now));
        mvc.perform(get("/api/v1/older-adults/{id}/status", owner).param("caregiverId", pendingCaregiver))
                .andExpect(status().isForbidden());
        var monitor = monitors.findByOlderAdultId(owner).orElseThrow();
        followUp.handle(new com.tata.carelink.domain.model.events.CareLinkConfirmed(monitor.getCareLinkId(), caregiver, owner));
        org.junit.jupiter.api.Assertions.assertEquals(monitor.getId(), monitors.findByOlderAdultId(owner).orElseThrow().getId());
        mvc.perform(get("/api/v1/older-adults/{id}/status", owner)).andExpect(status().isBadRequest());
    }
    @Test void contactReadsRegisteredEmergencyPhoneAndMissingContactReturnsNotFound() throws Exception {
        mvc.perform(get("/api/v1/older-adults/{id}/contact-channel", owner).param("caregiverId", caregiver))
                .andExpect(status().isNotFound());
        var now = Instant.now();
        profiles.save(com.tata.carelink.domain.model.aggregates.OlderAdultProfile.rehydrate(owner, caregiver,
                new com.tata.carelink.domain.model.valueobjects.OlderAdultBasicData("Older Adult", LocalDate.of(1950, 1, 1)),
                new com.tata.carelink.domain.model.valueobjects.EmergencyContact("Contact", "Family", "+51999999999"), now, now));
        mvc.perform(get("/api/v1/older-adults/{id}/contact-channel", owner).param("caregiverId", caregiver))
                .andExpect(status().isOk()).andExpect(jsonPath("$.value").value("+51999999999"));
    }

}


