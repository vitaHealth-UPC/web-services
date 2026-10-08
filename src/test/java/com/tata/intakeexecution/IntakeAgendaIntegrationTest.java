package com.tata.intakeexecution;

import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IntakeAgendaIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired IntakeRepository repository;
    MockMvc mvc;
    final Instant from = Instant.parse("2026-10-05T05:00:00Z");
    @BeforeEach void setUp() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }
    private Intake intake(String owner, Instant scheduled) {
        return Intake.createScheduled(UUID.randomUUID().toString(), UUID.randomUUID().toString(), owner,
                new MedicationSnapshot("Losartan", "1 tablet", "With water"), scheduled, from);
    }
    @Test void agendaIsOrderedScopedAndIncludesResolvedHistoryWithExclusiveEnd() throws Exception {
        var owner = UUID.randomUUID().toString();
        var first = intake(owner, from);
        first.confirm(from, ConfirmationChannel.TOUCH);
        repository.saveAll(List.of(intake(owner, from.plusSeconds(3600)), first,
                intake(owner, from.plusSeconds(86400)), intake(UUID.randomUUID().toString(), from), intake(owner, from.minusSeconds(1))));
        mvc.perform(get("/api/v1/older-adults/{id}/intakes/agenda", owner).param("from", from.toString()).param("to", from.plusSeconds(86400).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(first.id())).andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].confirmedAt").exists()).andExpect(jsonPath("$[1].status").value("PENDING"));
    }
    @Test void emptyAgendaReturnsAnEmptyArray() throws Exception {
        mvc.perform(get("/api/v1/older-adults/{id}/intakes/agenda", UUID.randomUUID()).param("from", from.toString()).param("to", from.plusSeconds(86400).toString()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
    @Test void rejectsReversedUnboundedAndMalformedRanges() throws Exception {
        var path = "/api/v1/older-adults/owner/intakes/agenda";
        mvc.perform(get(path).param("from", from.toString()).param("to", from.toString())).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("from", from.toString()).param("to", from.plusSeconds(9 * 86400).toString())).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("from", "invalid").param("to", from.toString())).andExpect(status().isBadRequest());
        mvc.perform(get(path)).andExpect(status().isBadRequest());
    }
}