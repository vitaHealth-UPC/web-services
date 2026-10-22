package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.familymonitoring.application.internal.commandservices.ProcessFamilyStockCatchUpCommandHandler;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.RecoverFamilyStockCatchUpsCommandHandler;
import com.vitahealth.tata.familymonitoring.application.internal.commandservices.RetryFamilyStockCatchUpCommandHandler;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.FamilyStockCatchUp;
import com.vitahealth.tata.familymonitoring.domain.repositories.FamilyStockCatchUpRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

/**
 * Verifies bounded recovery progresses beyond failed work and respects transaction attempt budgets.
 */
class FamilyStockCatchUpRecoveryBoundsTest {
  private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final FamilyStockCatchUpRepository work = mock(FamilyStockCatchUpRepository.class);
  private final ProcessFamilyStockCatchUpCommandHandler process =
      mock(ProcessFamilyStockCatchUpCommandHandler.class);
  private final RetryFamilyStockCatchUpCommandHandler retry =
      mock(RetryFamilyStockCatchUpCommandHandler.class);
  private final RecoverFamilyStockCatchUpsCommandHandler recovery =
      new RecoverFamilyStockCatchUpsCommandHandler(work, process, retry, clock);

  private void requests(int count) {
    var requests =
        LongStream.rangeClosed(1, count).mapToObj(id -> FamilyStockCatchUp.start(id, NOW)).toList();
    when(work.findDueAfter(any(Instant.class), anyLong(), anyInt()))
        .thenAnswer(
            invocation -> {
              long cursor = invocation.getArgument(1);
              int limit = invocation.getArgument(2);
              return requests.stream()
                  .filter(request -> request.monitorId() > cursor)
                  .limit(limit)
                  .toList();
            });
    for (var request : requests)
      when(work.find(request.monitorId())).thenReturn(Optional.of(request));
  }

  @Test
  void failedProjectionAndRetryMetadataDoNotPreventCrossingTheFirstPage() {
    requests(27);
    when(process.handle(1)).thenThrow(new IllegalStateException("controlled projection failure"));
    doThrow(new IllegalStateException("controlled retry storage failure"))
        .when(retry)
        .handle(FamilyStockCatchUp.start(1, NOW));
    recovery.handle(100);
    for (long id = 1; id <= 27; id++) verify(process).handle(id);
    verify(work).findDueAfter(NOW, 25, 25);
    verify(retry).handle(FamilyStockCatchUp.start(1, NOW));
  }

  @Test
  void globalBudgetPreventsStartingAdditionalRelationshipSteps() {
    requests(2);
    recovery.handle(1);
    verify(process).handle(1);
    verify(process, times(0)).handle(2);
  }

  @Test
  void oneRelationshipCannotConsumeMoreThanTenStepsOfTheRunBudget() {
    requests(2);
    when(process.handle(anyLong())).thenReturn(true);
    recovery.handle(12);
    verify(process, times(10)).handle(1);
    verify(process, times(2)).handle(2);
  }

  @Test
  void retryUsesConditionalSnapshotAndCapsExponentialBackoffAtOneHour() {
    var writer = new RetryFamilyStockCatchUpCommandHandler(work, clock);
    for (int attempts : List.of(0, 1, 7, 30)) {
      var expected = new FamilyStockCatchUp(1, "medication", NOW, NOW, attempts);
      writer.handle(expected);
      long delay = attempts == 0 ? 30 : attempts == 1 ? 60 : 3600;
      var replacement = expected.retryAt(NOW.plusSeconds(delay));
      verify(work).replace(expected, replacement);
      assertEquals("medication", replacement.afterMedicationId());
      assertEquals(NOW, replacement.requestedAt());
    }
  }
}
