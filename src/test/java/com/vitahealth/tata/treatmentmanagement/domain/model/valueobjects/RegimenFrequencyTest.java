package com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Preserves supported Spanish mobile labels and rejects unrecognized generation semantics. */
class RegimenFrequencyTest {
  @Test
  void recognizesDailyApiAndMobileAliasesWithStableLocale() {
    for (var label : List.of("DAILY", "daily", "Cada día", "Diaria", "  CADA   DÍA  "))
      assertEquals(RegimenFrequency.DAILY, RegimenFrequency.fromLabel(label));
  }

  @Test
  void onceDailyAliasesDoNotAcceptSeveralDailyTimes() {
    for (var label : List.of("Una vez al día", "Cada 24 horas")) {
      assertEquals(RegimenFrequency.ONCE_DAILY, RegimenFrequency.fromLabel(label));
      assertDoesNotThrow(() -> regimen(label, List.of(LocalTime.NOON)));
      assertThrows(
          IllegalArgumentException.class,
          () -> regimen(label, List.of(LocalTime.of(8, 0), LocalTime.of(20, 0))));
    }
    assertDoesNotThrow(() -> regimen("DAILY", List.of(LocalTime.of(8, 0), LocalTime.of(20, 0))));
  }

  @Test
  void intervalChoicesRequireTheirCompleteCongruentCycleIncludingMidnight() {
    assertDoesNotThrow(
        () ->
            regimen(
                "Cada 8 horas",
                List.of(LocalTime.of(22, 30), LocalTime.of(6, 30), LocalTime.of(14, 30))));
    assertDoesNotThrow(
        () -> regimen("Cada 12 horas", List.of(LocalTime.of(8, 0), LocalTime.of(20, 0))));
    assertThrows(
        IllegalArgumentException.class,
        () -> regimen("Cada 12 horas", List.of(LocalTime.of(8, 0), LocalTime.of(21, 0))));
    assertThrows(
        IllegalArgumentException.class, () -> regimen("Cada 8 horas", List.of(LocalTime.NOON)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            regimen(
                "Cada 8 horas",
                List.of(LocalTime.of(6, 30), LocalTime.of(14, 30), LocalTime.of(22, 31))));
  }

  private static TreatmentRegimen regimen(String label, List<LocalTime> times) {
    return new TreatmentRegimen("medication", "1 comprimido", label, times, "", 0);
  }

  @Test
  void recognizesOtherActualMobileChoicesWithoutTreatingThemAsDaily() {
    assertEquals(RegimenFrequency.EVERY_EIGHT_HOURS, RegimenFrequency.fromLabel("Cada 8 horas"));
    assertEquals(RegimenFrequency.EVERY_TWELVE_HOURS, RegimenFrequency.fromLabel("Cada 12 horas"));
    assertEquals(
        RegimenFrequency.PRESCRIBED_TIMES, RegimenFrequency.fromLabel("Según indicación médica"));
  }

  @Test
  void unknownFrequencyCannotEnterAConfiguredRegimen() {
    for (var label : List.of("weekly", "sometimes", "Cada 0 horas", "Cada 7 horas", ""))
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new TreatmentRegimen(
                  "medication", "1 comprimido", label, List.of(LocalTime.NOON), "", 0));
  }

  @Test
  void validationPreservesTheUserFacingLabelAndExplicitTimes() {
    var regimen =
        new TreatmentRegimen(
            "medication",
            "1 comprimido",
            "Cada 12 horas",
            List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            "",
            0);
    assertEquals("Cada 12 horas", regimen.frequency());
    assertEquals(2, regimen.scheduledTimes().size());
  }
}
