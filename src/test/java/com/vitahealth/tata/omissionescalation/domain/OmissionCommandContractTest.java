package com.vitahealth.tata.omissionescalation.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.omissionescalation.domain.model.commands.*;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Rejects incomplete requests before a handler can reserve or mutate stored evidence. */
class OmissionCommandContractTest {
  private static final Instant NOW = Instant.parse("2026-10-17T12:00:00.123456Z");

  @Test
  void allCaseCommandsRequirePositiveIdentity() {
    List<Function<Long, Object>> constructors =
        List.of(
            id -> new CloseOmissionCaseCommand(id, NOW),
            id -> new EscalateOmissionCommand(id, NOW),
            id -> new GenerateCaregiverAlertCommand(id, NOW),
            id -> new RegisterOmissionCommand(id, NOW),
            SendReinforcedReminderCommand::new);
    for (var create : constructors) {
      assertThrows(IllegalArgumentException.class, () -> create.apply(null));
      assertThrows(IllegalArgumentException.class, () -> create.apply(0L));
      assertThrows(IllegalArgumentException.class, () -> create.apply(-1L));
      assertNotNull(create.apply(1L));
    }
  }

  @Test
  void occurrenceTimeIsRequiredAndExplicitValueIsPreserved() {
    assertThrows(IllegalArgumentException.class, () -> new CloseOmissionCaseCommand(1L, null));
    assertThrows(IllegalArgumentException.class, () -> new EscalateOmissionCommand(1L, null));
    assertThrows(IllegalArgumentException.class, () -> new GenerateCaregiverAlertCommand(1L, null));
    assertThrows(IllegalArgumentException.class, () -> new RegisterOmissionCommand(1L, null));
    assertThrows(IllegalArgumentException.class, () -> new EvaluateGracePeriodCommand(null));
    assertEquals(NOW, new EvaluateGracePeriodCommand(NOW).now());
    assertEquals(NOW, new ResolveOmissionCaseCommand("intake", NOW).confirmedAt());
  }

  @Test
  void sourceReferencesAreRequiredAndNormalizedWithoutChangingRecordedTime() {
    var command = new OpenOmissionCaseCommand(" intake ", " adult ", " Losartan ", NOW);
    assertEquals("intake", command.intakeId());
    assertEquals("adult", command.olderAdultId());
    assertEquals("Losartan", command.medicationName());
    assertEquals(NOW, command.scheduledAt());
    assertThrows(
        IllegalArgumentException.class,
        () -> new OpenOmissionCaseCommand(" ", "adult", "Losartan", NOW));
    assertThrows(
        IllegalArgumentException.class,
        () -> new OpenOmissionCaseCommand("intake", "adult", "Losartan", null));
    assertThrows(IllegalArgumentException.class, () -> new ResolveOmissionCaseCommand(" ", NOW));
    assertThrows(
        IllegalArgumentException.class, () -> new ResolveOmissionCaseCommand("intake", null));
  }
}
