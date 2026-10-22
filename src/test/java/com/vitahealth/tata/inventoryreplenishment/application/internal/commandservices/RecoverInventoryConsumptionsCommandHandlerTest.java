package com.vitahealth.tata.inventoryreplenishment.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects.PendingInventoryConsumption;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryConsumptionRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks bounded scan coordination independently of storage and per-item transaction mechanics. */
class RecoverInventoryConsumptionsCommandHandlerTest {
  private static final Instant NOW = Instant.parse("2026-10-20T00:00:00Z");
  private final InventoryConsumptionRequestRepository requests =
      mock(InventoryConsumptionRequestRepository.class);
  private final ProcessInventoryConsumptionCommandHandler process =
      mock(ProcessInventoryConsumptionCommandHandler.class);
  private final RecordInventoryConsumptionRetryCommandHandler retries =
      mock(RecordInventoryConsumptionRetryCommandHandler.class);
  private final RecoverInventoryConsumptionsCommandHandler recovery =
      new RecoverInventoryConsumptionsCommandHandler(
          requests, process, retries, Clock.fixed(NOW, ZoneOffset.UTC));

  private PendingInventoryConsumption item(String id) {
    return PendingInventoryConsumption.request(
        new RequestInventoryConsumptionCommand(id, "medicine", "adult", NOW), NOW);
  }

  private RequestInventoryConsumptionCommand command(PendingInventoryConsumption item) {
    return new RequestInventoryConsumptionCommand(
        item.intakeId(), item.medicationId(), item.olderAdultId(), item.confirmedAt());
  }

  @Test
  void failingItemAndRetryMetadataFailureDoNotStarveHealthyItemsOrChangeCursor() {
    var first = item("first");
    var second = item("second");
    when(requests.findDueAfter(NOW, null, 25)).thenReturn(List.of(first, second));
    when(requests.findDueAfter(NOW, second, 25)).thenReturn(List.of());
    doThrow(new IllegalStateException("processing failed")).when(process).handle(command(first));
    doThrow(new IllegalStateException("metadata failed"))
        .when(retries)
        .handle(first, NOW.plusSeconds(30));
    recovery.handle(100);
    verify(process).handle(command(first));
    verify(process).handle(command(second));
    verify(retries).handle(first, NOW.plusSeconds(30));
    verify(requests).findDueAfter(NOW, second, 25);
    verifyNoMoreInteractions(process, retries);
  }

  @Test
  void maximumItemsBoundsScanAndProcessing() {
    var first = item("first");
    when(requests.findDueAfter(NOW, null, 1)).thenReturn(List.of(first));
    recovery.handle(1);
    verify(requests).findDueAfter(NOW, null, 1);
    verify(process).handle(command(first));
    verifyNoMoreInteractions(requests, process);
    verifyNoInteractions(retries);
    assertThrows(IllegalArgumentException.class, () -> recovery.handle(0));
    assertThrows(IllegalArgumentException.class, () -> recovery.handle(1001));
  }

  @Test
  void attemptUsesStoredEvidenceAndCapsRetryBackoff() {
    var original = item("source");
    var repeated =
        new PendingInventoryConsumption(
            original.intakeId(),
            original.medicationId(),
            original.olderAdultId(),
            NOW,
            NOW,
            NOW,
            100);
    when(requests.findByIntakeId("source")).thenReturn(java.util.Optional.of(repeated));
    doThrow(new IllegalStateException("processing failed")).when(process).handle(any());
    recovery.attempt(command(repeated));
    verify(retries).handle(repeated, NOW.plusSeconds(3600));
  }
}
