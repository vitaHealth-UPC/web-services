package com.vitahealth.tata.inventoryreplenishment.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects.PendingInventoryConsumption;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Protects original confirmation evidence from conflicting replays and retry metadata changes. */
class PendingInventoryConsumptionTest {
  private static final Instant CONFIRMED = Instant.parse("2026-10-20T05:00:00Z");
  private static final Instant CREATED = CONFIRMED.plusSeconds(1);

  private RequestInventoryConsumptionCommand command() {
    return new RequestInventoryConsumptionCommand("intake", "medicine", "adult", CONFIRMED);
  }

  @Test
  void retriesRetainExactSourceEvidenceAndPreviousSnapshot() {
    var original = PendingInventoryConsumption.request(command(), CREATED);
    var retry = original.retryAt(CREATED.plusSeconds(60));
    assertTrue(retry.matches(command()));
    assertEquals(CONFIRMED, retry.confirmedAt());
    assertEquals(CREATED, retry.createdAt());
    assertEquals(1, retry.attempts());
    assertEquals(0, original.attempts());
    assertEquals(CREATED, original.nextAttemptAt());
  }

  @Test
  void changedSourceFieldsNeverMatchADuplicateDelivery() {
    var pending = PendingInventoryConsumption.request(command(), CREATED);
    assertFalse(
        pending.matches(
            new RequestInventoryConsumptionCommand("other", "medicine", "adult", CONFIRMED)));
    assertFalse(
        pending.matches(
            new RequestInventoryConsumptionCommand("intake", "other", "adult", CONFIRMED)));
    assertFalse(
        pending.matches(
            new RequestInventoryConsumptionCommand("intake", "medicine", "other", CONFIRMED)));
    assertFalse(
        pending.matches(
            new RequestInventoryConsumptionCommand(
                "intake", "medicine", "adult", CONFIRMED.plusNanos(1))));
  }

  @Test
  void invalidRetryMetadataCannotAlterOriginalEvidence() {
    var pending = PendingInventoryConsumption.request(command(), CREATED);
    assertThrows(IllegalArgumentException.class, () -> pending.retryAt(CONFIRMED));
    assertThrows(NullPointerException.class, () -> pending.retryAt(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> PendingInventoryConsumption.request(command(), CONFIRMED.minusNanos(1)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PendingInventoryConsumption(
                "intake", "medicine", "adult", CONFIRMED, CREATED, CREATED, -1));
    var exhausted =
        new PendingInventoryConsumption(
            "intake", "medicine", "adult", CONFIRMED, CREATED, CREATED, Integer.MAX_VALUE);
    assertThrows(ArithmeticException.class, () -> exhausted.retryAt(CREATED.plusSeconds(1)));
    assertEquals(Integer.MAX_VALUE, exhausted.attempts());
    assertEquals(0, pending.attempts());
  }

  @Test
  void sourceReferencesAreRequiredAndNormalizedWithoutRoundingTime() {
    var precise = CONFIRMED.plusNanos(123);
    var command =
        new RequestInventoryConsumptionCommand(" intake ", " medicine ", " adult ", precise);
    assertEquals("intake", command.intakeId());
    assertEquals("medicine", command.medicationId());
    assertEquals("adult", command.olderAdultId());
    assertEquals(precise, command.confirmedAt());
    assertThrows(
        IllegalArgumentException.class,
        () -> new RequestInventoryConsumptionCommand(" ", "medicine", "adult", CONFIRMED));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RequestInventoryConsumptionCommand("intake", null, "adult", CONFIRMED));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RequestInventoryConsumptionCommand("intake", "medicine", " ", CONFIRMED));
    assertThrows(
        NullPointerException.class,
        () -> new RequestInventoryConsumptionCommand("intake", "medicine", "adult", null));
  }
}
