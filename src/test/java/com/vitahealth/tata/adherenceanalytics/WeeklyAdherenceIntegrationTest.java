package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import com.vitahealth.tata.shared.fixtures.ConsentedAdultHttpFixture;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WeeklyAdherenceIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired IntakeRepository repository;
  @Autowired IntakeContextFacade intakes;
  MockMvc mvc;
  String owner;
  final Instant from = Instant.parse("2026-10-05T05:00:00Z");

  @BeforeEach
  void setup() {
    var fixture = ConsentedAdultHttpFixture.create(context);
    owner = fixture.owner();
    mvc = fixture.mvc();
  }

  private Intake intake(String owner, Instant scheduled) {
    return Intake.createScheduled(
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        owner,
        new MedicationSnapshot("Medication", "1 tablet", ""),
        scheduled,
        from);
  }

  @Test
  void countsDefinitiveOutcomesAndIsolatesOwnerAndRange() throws Exception {

    var confirmed = intake(owner, from);
    confirmed.confirm(from, ConfirmationChannel.TOUCH);
    var late = intake(owner, from.plusSeconds(60));
    late.confirm(from.plusSeconds(120), ConfirmationChannel.TOUCH);
    var omitted = intake(owner, from.plusSeconds(180));
    omitted.markOmitted();
    repository.saveAll(
        List.of(
            confirmed,
            late,
            omitted,
            intake(owner, from.plusSeconds(240)),
            intake(owner, from.plusSeconds(86400)),
            intake(UUID.randomUUID().toString(), from)));
    mvc.perform(
            get("/api/v1/older-adults/{id}/adherence/weekly", owner)
                .param("from", from.toString())
                .param("to", from.plusSeconds(86400).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.confirmedIntakes").value(2))
        .andExpect(jsonPath("$.totalIntakes").value(3))
        .andExpect(jsonPath("$.onTimeIntakes").value(1))
        .andExpect(jsonPath("$.lateIntakes").value(1))
        .andExpect(jsonPath("$.omittedIntakes").value(1))
        .andExpect(
            jsonPath("$.percentage").value(org.hamcrest.Matchers.closeTo(66.6666667, .0001)));
    mvc.perform(
            get("/api/v1/older-adults/{id}/adherence/history", owner)
                .param("from", from.toString())
                .param("to", from.plusSeconds(86400).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  void emptyEvidenceReturnsZeroWithoutDivisionByZero() throws Exception {
    mvc.perform(
            get("/api/v1/older-adults/{id}/adherence/weekly", owner)
                .param("from", from.toString())
                .param("to", from.plusSeconds(86400).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalIntakes").value(0))
        .andExpect(jsonPath("$.percentage").value(0.0));
  }

  @Test
  void rejectsInvalidBounds() throws Exception {
    String path = "/api/v1/older-adults/" + owner + "/adherence/weekly";
    mvc.perform(get(path).param("from", from.toString()).param("to", from.toString()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get(path)
                .param("from", from.toString())
                .param("to", from.plusSeconds(9 * 86400).toString()))
        .andExpect(status().isBadRequest());
    mvc.perform(get(path).param("from", "invalid").param("to", from.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patternsRequireThreeDistinctOmissionDaysAndValidateZone() throws Exception {

    String medication = UUID.randomUUID().toString();
    for (int day = 0; day < 3; day++) {
      var omitted =
          Intake.createScheduled(
              UUID.randomUUID().toString(),
              medication,
              owner,
              new MedicationSnapshot("Medication", "1 tablet", ""),
              from.plusSeconds(day * 86400L),
              from);
      omitted.markOmitted();
      repository.saveAll(List.of(omitted));
    }
    String path = "/api/v1/older-adults/{id}/adherence/patterns";
    mvc.perform(
            get(path, owner)
                .param("from", from.toString())
                .param("to", from.plusSeconds(7 * 86400).toString())
                .param("zone", "America/Bogota"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].medicationId").value(medication))
        .andExpect(jsonPath("$[0].omissionDays").value(3));
    mvc.perform(
            get("/api/v1/older-adults/{id}/adherence/recommendations", owner)
                .param("from", from.toString())
                .param("to", from.plusSeconds(7 * 86400).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("REVIEW_REMINDER_AND_CAREGIVER_FOLLOW_UP"))
        .andExpect(jsonPath("$[0].evidenceDays").value(3));
    mvc.perform(
            get(path, owner)
                .param("from", from.toString())
                .param("to", from.plusSeconds(86400).toString())
                .param("zone", "invalid-zone"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void ownerDiscoveryPagesDistinctActualEvidenceWithoutSkippingTheTail() {
    var start = Instant.parse("2035-10-01T00:00:00Z");
    var end = start.plusSeconds(86400);
    var prefix = UUID.randomUUID().toString().substring(0, 8);
    var expected = new ArrayList<String>();
    var records = new ArrayList<Intake>();
    for (int index = 0; index < 105; index++) {
      var adult = prefix + String.format("-%03d", index);
      expected.add(adult);
      records.add(intake(adult, start));
    }
    records.add(intake(expected.getFirst(), start.plusSeconds(60)));
    records.add(intake(prefix + "-outside", end));
    repository.saveAll(records);
    var first = intakes.findOlderAdultIdsWithIntakes(start, end, null, 100);
    var second = intakes.findOlderAdultIdsWithIntakes(start, end, first.getLast(), 100);
    var actual = new ArrayList<>(first);
    actual.addAll(second);
    assertEquals(100, first.size());
    assertEquals(5, second.size());
    assertEquals(expected, actual);
    assertTrue(intakes.findOlderAdultIdsWithIntakes(start, end, second.getLast(), 100).isEmpty());
  }
}
