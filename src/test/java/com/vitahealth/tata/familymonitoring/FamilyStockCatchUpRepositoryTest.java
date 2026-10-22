package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.FamilyStockCatchUp;
import com.vitahealth.tata.familymonitoring.domain.repositories.FamilyStockCatchUpRepository;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies committed cursor recovery, rollback, bounded scans and stale-writer protection. */
@SpringBootTest
@ActiveProfiles("test")
class FamilyStockCatchUpRepositoryTest {
  @Autowired FamilyStockCatchUpRepository work;
  @Autowired IFamilyMonitorRepository monitors;
  @Autowired PlatformTransactionManager transactions;
  @Autowired Clock clock;

  private FamilyStockCatchUp request() {
    var monitor =
        monitors.save(
            new FamilyMonitor(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()));
    return FamilyStockCatchUp.start(monitor.getId(), clock.instant());
  }

  private TransactionTemplate transaction() {
    return new TransactionTemplate(transactions);
  }

  @Test
  void creationAndProgressRequireCallerTransactionAndRollbackTogether() {
    var request = request();
    assertThrows(IllegalTransactionStateException.class, () -> work.enqueue(request));
    assertThrows(IllegalTransactionStateException.class, () -> work.reserve(request.monitorId()));
    transaction()
        .executeWithoutResult(
            status -> {
              work.enqueue(request);
              status.setRollbackOnly();
            });
    assertTrue(work.find(request.monitorId()).isEmpty());
    transaction().executeWithoutResult(status -> work.enqueue(request));
    var advanced = request.advance("medication-1", request.requestedAt());
    transaction()
        .executeWithoutResult(
            status -> {
              assertTrue(work.replace(request, advanced));
              status.setRollbackOnly();
            });
    assertEquals(request, work.find(request.monitorId()).orElseThrow());
    transaction().executeWithoutResult(status -> work.complete(request));
  }

  @Test
  void staleRetryCannotResetAdvancedCursorOrResurrectCompletedWork() {
    var request = request();
    transaction().executeWithoutResult(status -> work.enqueue(request));
    var advanced = request.advance("medication-1", request.requestedAt());
    transaction().executeWithoutResult(status -> assertTrue(work.replace(request, advanced)));
    transaction().executeWithoutResult(status -> work.enqueue(request));
    assertEquals(advanced, work.find(request.monitorId()).orElseThrow());
    transaction()
        .executeWithoutResult(
            status ->
                assertFalse(
                    work.replace(request, request.retryAt(request.requestedAt().plusSeconds(30)))));
    transaction().executeWithoutResult(status -> assertTrue(work.complete(advanced)));
    transaction()
        .executeWithoutResult(
            status ->
                assertFalse(
                    work.replace(
                        advanced, advanced.retryAt(advanced.requestedAt().plusSeconds(30)))));
    assertTrue(work.find(request.monitorId()).isEmpty());
  }

  @Test
  void dueScanUsesExclusiveIdentityAndSkipsFutureRetries() {
    var first = request();
    var future = request();
    var last = request();
    transaction()
        .executeWithoutResult(
            status -> {
              work.enqueue(first);
              work.enqueue(future);
              work.enqueue(last);
              work.replace(future, future.retryAt(future.requestedAt().plusSeconds(3600)));
            });
    var cutoff = clock.instant();
    var page = work.findDueAfter(cutoff, first.monitorId() - 1, 1);
    assertEquals(List.of(first), page);
    assertEquals(List.of(last), work.findDueAfter(cutoff, first.monitorId(), 1));
    assertInstanceOf(
        IllegalArgumentException.class,
        assertThrows(
                InvalidDataAccessApiUsageException.class, () -> work.findDueAfter(cutoff, 0, 101))
            .getCause());
    assertInstanceOf(
        IllegalArgumentException.class,
        assertThrows(
                InvalidDataAccessApiUsageException.class, () -> work.findDueAfter(cutoff, -1, 1))
            .getCause());
    transaction()
        .executeWithoutResult(
            status -> {
              work.complete(first);
              work.complete(last);
              work.complete(work.reserve(future.monitorId()).orElseThrow());
            });
  }
}
