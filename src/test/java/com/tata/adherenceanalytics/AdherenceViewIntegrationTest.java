package com.tata.adherenceanalytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdherenceViewIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired IntakeRepository repository;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private Intake intake(String owner, Instant scheduled, String medication) {
        return Intake.createScheduled(UUID.randomUUID().toString(), UUID.randomUUID().toString(), owner,
                new MedicationSnapshot(medication, "1 tablet", ""), scheduled, Instant.now());
    }

    @Test
    void summaryReturnsTheDefinitiveOutcomesWithMedicationNames() throws Exception {
        String owner = UUID.randomUUID().toString();
        Instant now = Instant.now();
        var confirmed = intake(owner, now.minus(Duration.ofHours(3)), "Losartán");
        confirmed.confirm(confirmed.scheduledAt(), ConfirmationChannel.TOUCH);
        var late = intake(owner, now.minus(Duration.ofHours(2)), "Amlodipino");
        late.confirm(late.scheduledAt().plusSeconds(60), ConfirmationChannel.TOUCH);
        var omitted = intake(owner, now.minus(Duration.ofHours(1)), "Vitamina D3");
        omitted.markOmitted();
        repository.saveAll(List.of(confirmed, late, omitted,
                intake(owner, now.plus(Duration.ofHours(5)), "Pendiente")));

        mvc.perform(get("/api/v1/older-adults/{id}/adherence/summary", owner).param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodDays").value(7))
                .andExpect(jsonPath("$.scheduledCount").value(3))
                .andExpect(jsonPath("$.adherencePercent").value(67))
                .andExpect(jsonPath("$.onTimePercent").value(33))
                .andExpect(jsonPath("$.lateCount").value(1))
                .andExpect(jsonPath("$.omittedCount").value(1))
                .andExpect(jsonPath("$.recentIntakes.length()").value(3))
                .andExpect(jsonPath("$.recentIntakes[0].medicationName").value("Vitamina D3"))
                .andExpect(jsonPath("$.recentIntakes[0].status").value("OMITTED"))
                .andExpect(jsonPath("$.recentIntakes[1].status").value("LATE"))
                .andExpect(jsonPath("$.recentIntakes[2].medicationName").value("Losartán"));
    }

    @Test
    void summaryAndInsightsReturnNoContentWithoutEvidence() throws Exception {
        String owner = UUID.randomUUID().toString();
        mvc.perform(get("/api/v1/older-adults/{id}/adherence/summary", owner)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/older-adults/{id}/adherence/insight", owner)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/older-adults/{id}/adherence/insights", owner)).andExpect(status().isNoContent());
    }

    @Test
    void rejectsInvalidPeriodAndZone() throws Exception {
        String path = "/api/v1/older-adults/owner/adherence/summary";
        mvc.perform(get(path).param("days", "0")).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("days", "99")).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("days", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("zone", "Nowhere/City")).andExpect(status().isBadRequest());
    }
}
