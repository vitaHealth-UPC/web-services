package com.vitahealth.tata.treatmentmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.treatmentmanagement.application.internal.commandservices.ExtendTreatmentScheduleCommandHandler;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.vitahealth.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.vitahealth.tata.treatmentmanagement.domain.model.commands.ExtendTreatmentScheduleCommand;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRecurrence;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentRegimen;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.vitahealth.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Extends real persisted schedules across a clock advance and a daylight-saving transition. */
@SpringBootTest
@ActiveProfiles("test")
class TreatmentIntervalHorizonIntegrationTest {
  @Autowired MedicationRepository medications;
  @Autowired TreatmentRepository treatments;
  @Autowired IntakeRepository intakes;
  @Autowired ExtendTreatmentScheduleCommandHandler extension;

  @MockitoBean(enforceOverride = true)
  Clock clock;

  @Test
  void extensionRetainsAnchorExistingIdentitiesAndConfirmedHistoryAcrossDst() {
    var initial = Instant.parse("2026-03-06T12:00:00Z");
    var anchor = Instant.parse("2026-03-06T13:00:00Z");
    when(clock.instant()).thenReturn(initial);
    var adult = UUID.randomUUID().toString();
    var medication =
        medications.save(Medication.register(adult, "Losartán", "Comprimido", initial));
    var treatment = Treatment.create(adult, "Presión", initial);
    treatment.configure(
        new TreatmentRegimen(
            medication.id(),
            "1 comprimido",
            "cada 8 horas",
            List.of(LocalTime.of(5, 0), LocalTime.of(13, 0), LocalTime.of(21, 0)),
            "",
            10,
            new TreatmentRecurrence(
                TreatmentRecurrence.Mode.FIXED_INTERVAL, "America/New_York", anchor)));
    treatment.activate();
    treatments.save(treatment);
    assertTrue(extension.handle(new ExtendTreatmentScheduleCommand(treatment.id())));
    var before = intakes.findFutureByTreatmentId(treatment.id(), initial);
    var confirmed = before.getFirst();
    confirmed.confirm(anchor, ConfirmationChannel.TOUCH);
    intakes.saveAll(List.of(confirmed));
    var oldLast = before.getLast().scheduledAt();

    var later = Instant.parse("2026-03-10T14:00:00Z");
    when(clock.instant()).thenReturn(later);
    assertTrue(extension.handle(new ExtendTreatmentScheduleCommand(treatment.id())));
    var after = intakes.findFutureByTreatmentId(treatment.id(), initial);
    assertTrue(after.getLast().scheduledAt().isAfter(oldLast));
    for (var original : before) {
      var retained =
          after.stream()
              .filter(value -> value.id().equals(original.id()))
              .findFirst()
              .orElseThrow();
      assertEquals(original.scheduledAt(), retained.scheduledAt());
    }
    assertEquals(anchor, intakes.findById(confirmed.id()).orElseThrow().confirmedAt());
    var pending = intakes.findFutureByTreatmentId(treatment.id(), later);
    for (var intake : pending)
      assertEquals(
          0,
          Duration.between(anchor, intake.scheduledAt()).toSeconds()
              % Duration.ofHours(8).toSeconds());
    for (int i = 1; i < pending.size(); i++)
      assertEquals(
          Duration.ofHours(8),
          Duration.between(pending.get(i - 1).scheduledAt(), pending.get(i).scheduledAt()));
    assertEquals(
        anchor, treatments.findById(treatment.id()).orElseThrow().regimen().recurrence().anchor());
  }
}
