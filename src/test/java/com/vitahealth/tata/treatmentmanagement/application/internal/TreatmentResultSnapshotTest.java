package com.vitahealth.tata.treatmentmanagement.application.internal;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.treatmentmanagement.application.result.TreatmentResult;
import com.vitahealth.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Protects application projections without changing the draft transport contract. */
class TreatmentResultSnapshotTest {
  @Test
  void projectionOwnsItsScheduleSnapshot() {
    var times = new ArrayList<>(List.of(LocalTime.NOON));
    var result =
        new TreatmentResult(
            "treatment",
            "adult",
            "Treatment",
            TreatmentStatus.ACTIVE,
            "medication",
            "50 mg",
            "daily",
            times,
            "water",
            10);
    times.clear();
    assertEquals(List.of(LocalTime.NOON), result.scheduledTimes());
    assertThrows(UnsupportedOperationException.class, () -> result.scheduledTimes().clear());
  }

  @Test
  void draftWithoutRegimenKeepsAbsentTimes() {
    var result =
        new TreatmentResult(
            "treatment",
            "adult",
            "Treatment",
            TreatmentStatus.DRAFT,
            null,
            null,
            null,
            null,
            null,
            null);
    assertNull(result.scheduledTimes());
  }
}
