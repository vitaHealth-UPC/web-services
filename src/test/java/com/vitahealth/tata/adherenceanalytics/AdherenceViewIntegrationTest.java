package com.vitahealth.tata.adherenceanalytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.shared.fixtures.ConsentedAdultHttpFixture;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdherenceViewIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired IntakeRepository repository;

  @Autowired
  com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository treatments;

  MockMvc mvc;
  String owner;

  @BeforeEach
  void setup() {
    var fixture = ConsentedAdultHttpFixture.create(context);
    owner = fixture.owner();
    mvc = fixture.mvc();
  }

  private Intake intake(String owner, Instant scheduled, String medication) {
    return Intake.createScheduled(
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        owner,
        new MedicationSnapshot(medication, "1 tablet", ""),
        scheduled,
        Instant.now());
  }

  @Test
  void treatmentScopeSeparatesRegimensAndRetainsLateAsAdherent() throws Exception {
    var now = Instant.now();
    var selected =
        treatments.save(
            com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment.create(
                owner, "Presion", now));
    var other =
        treatments.save(
            com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment.create(
                owner, "Vitaminas", now));
    var late =
        Intake.createScheduled(
            selected.id(),
            UUID.randomUUID().toString(),
            owner,
            new MedicationSnapshot("Losartan", "1 tablet", ""),
            now.minusSeconds(3600),
            now);
    late.confirm(late.scheduledAt().plusSeconds(60), ConfirmationChannel.TOUCH);
    var omitted =
        Intake.createScheduled(
            other.id(),
            UUID.randomUUID().toString(),
            owner,
            new MedicationSnapshot("Vitamina", "1 tablet", ""),
            now.minusSeconds(1800),
            now);
    omitted.markOmitted();
    repository.saveAll(List.of(late, omitted));
    var path = "/api/v1/older-adults/" + owner + "/adherence/summary";
    mvc.perform(
            get(
                "/api/v1/older-adults/"
                    + owner
                    + "/adherence/treatments/"
                    + selected.id()
                    + "/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheduledCount").value(1))
        .andExpect(jsonPath("$.adherencePercent").value(100));
    mvc.perform(get(path).param("days", "7").param("treatmentId", selected.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheduledCount").value(1))
        .andExpect(jsonPath("$.adherencePercent").value(100))
        .andExpect(jsonPath("$.onTimePercent").value(0))
        .andExpect(jsonPath("$.lateCount").value(1))
        .andExpect(jsonPath("$.omittedCount").value(0))
        .andExpect(jsonPath("$.recentIntakes.length()").value(1));
    mvc.perform(get(path).param("treatmentId", other.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.adherencePercent").value(0));
    var empty =
        treatments.save(
            com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment.create(
                owner, "Sin tomas", now));
    mvc.perform(get(path).param("treatmentId", empty.id())).andExpect(status().isNoContent());
    var foreign =
        treatments.save(
            com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment.create(
                UUID.randomUUID().toString(), "Ajeno", now));
    mvc.perform(get(path).param("treatmentId", foreign.id())).andExpect(status().isForbidden());
  }

  @Test
  void summaryReturnsTheDefinitiveOutcomesWithMedicationNames() throws Exception {

    Instant now = Instant.now();
    var confirmed = intake(owner, now.minus(Duration.ofHours(3)), "Losartán");
    confirmed.confirm(confirmed.scheduledAt(), ConfirmationChannel.TOUCH);
    var late = intake(owner, now.minus(Duration.ofHours(2)), "Amlodipino");
    late.confirm(late.scheduledAt().plusSeconds(60), ConfirmationChannel.TOUCH);
    var omitted = intake(owner, now.minus(Duration.ofHours(1)), "Vitamina D3");
    omitted.markOmitted();
    repository.saveAll(
        List.of(
            confirmed, late, omitted, intake(owner, now.plus(Duration.ofHours(5)), "Pendiente")));

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

    mvc.perform(get("/api/v1/older-adults/{id}/adherence/summary", owner))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/older-adults/{id}/adherence/insight", owner))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/older-adults/{id}/adherence/insights", owner))
        .andExpect(status().isNoContent());
  }

  @Test
  void signedAdultCannotReadAnotherAdultsAdherenceOrUnknownTreatmentEvidence()
      throws Exception {
    var otherAdult = UUID.randomUUID().toString();
    var otherTreatment = UUID.randomUUID().toString();
    var now = Instant.now();

    mvc.perform(get("/api/v1/older-adults/{id}/adherence/summary", otherAdult))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/older-adults/{id}/adherence/insights", otherAdult))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/older-adults/{id}/adherence/history", otherAdult)
            .param("from", now.minusSeconds(3600).toString())
            .param("to", now.toString()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/older-adults/{id}/adherence/summary", owner)
            .param("treatmentId", otherTreatment))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/older-adults/{id}/adherence/treatments/{treatmentId}/summary",
            owner, otherTreatment))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsInvalidPeriodAndZone() throws Exception {
    String path = "/api/v1/older-adults/" + owner + "/adherence/summary";
    mvc.perform(get(path).param("days", "0")).andExpect(status().isBadRequest());
    mvc.perform(get(path).param("days", "99")).andExpect(status().isBadRequest());
    mvc.perform(get(path).param("days", "abc")).andExpect(status().isBadRequest());
    mvc.perform(get(path).param("zone", "Nowhere/City")).andExpect(status().isBadRequest());
  }
}
