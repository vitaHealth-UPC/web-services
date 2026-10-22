package com.vitahealth.tata.omissionescalation.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.entities.EscalationRecord;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Verifies history order comes from business levels and preserves original stored evidence. */
class EscalationHistoryTest {
  private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");

  private static EscalationRecord record(long id, int level, Instant time) {
    return EscalationRecord.rehydrate(id, new EscalationLevel(level), " original reason ", time);
  }

  private static OmissionCase restore(List<EscalationRecord> records) {
    return OmissionCase.rehydrate(
        1L,
        "intake",
        "adult",
        "Losartan",
        NOW.minusSeconds(3600),
        OmissionCaseStatus.ESCALATED,
        GracePeriod.startingAt(NOW.minusSeconds(3600), Duration.ofMinutes(30)),
        null,
        NOW,
        null,
        List.of(),
        records,
        NOW,
        NOW);
  }

  @Test
  void shuffledHistoryIsOrderedWithoutChangingEvidenceOrCallerList() {
    var first = record(11, 1, NOW.plusSeconds(60));
    var second = record(22, 2, NOW.plusSeconds(120));
    var input = new ArrayList<>(List.of(second, first));
    var value = restore(input);
    assertEquals(2, value.currentLevel().value());
    assertEquals(second.getTriggeredAt(), value.lastActivityAt());
    assertSame(first, value.getEscalations().getFirst());
    assertEquals(" original reason ", first.getReason());
    assertEquals(List.of(second, first), input);
    assertEquals(22L, value.getEscalations().getLast().getId());
  }

  @Test
  void equalTimesUseDistinctIncreasingLevels() {
    var value = restore(List.of(record(22, 2, NOW), record(11, 1, NOW)));
    assertEquals(2, value.currentLevel().value());
    assertEquals(NOW, value.lastActivityAt());
  }

  @Test
  void duplicateSkippedAndReversedHistoryAreRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(List.of(record(1, 1, NOW), record(2, 1, NOW))));
    assertThrows(IllegalArgumentException.class, () -> restore(List.of(record(2, 2, NOW))));
    assertThrows(
        IllegalArgumentException.class,
        () -> restore(List.of(record(1, 1, NOW.plusSeconds(60)), record(2, 2, NOW))));
    assertThrows(
        IllegalArgumentException.class, () -> restore(List.of(record(1, 1, NOW.minusSeconds(1)))));
  }

  @Test
  void invalidRecordsAndBackwardLiveEscalationCannotAlterHistory() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new EscalationRecord(EscalationLevel.NONE, "reason", NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EscalationRecord(new EscalationLevel(1), " ", NOW));
    assertThrows(IllegalArgumentException.class, () -> record(0, 1, NOW));
    var value = restore(List.of(record(1, 1, NOW.plusSeconds(60))));
    assertThrows(IllegalArgumentException.class, () -> value.escalate("reason", NOW));
    assertEquals(1, value.currentLevel().value());
    assertEquals(1, value.getEscalations().size());
  }
}
