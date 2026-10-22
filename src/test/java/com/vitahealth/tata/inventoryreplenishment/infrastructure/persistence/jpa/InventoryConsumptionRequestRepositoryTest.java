package com.vitahealth.tata.inventoryreplenishment.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.inventoryreplenishment.domain.model.commands.RequestInventoryConsumptionCommand;
import com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects.PendingInventoryConsumption;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryConsumptionRequestRepository;
import java.time.Instant;
import java.util.ArrayList;
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

/** Verifies committed request storage and retry changes using independent real transactions. */
@SpringBootTest
@ActiveProfiles("test")
class InventoryConsumptionRequestRepositoryTest {
  private static final Instant NOW = Instant.parse("2020-01-01T00:00:00Z");
  @Autowired InventoryConsumptionRequestRepository requests;
  @Autowired PlatformTransactionManager transactions;

  private PendingInventoryConsumption request(Instant due) {
    return PendingInventoryConsumption.request(
        new RequestInventoryConsumptionCommand(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            due.minusSeconds(1)),
        due);
  }

  private void write(Runnable action) {
    new TransactionTemplate(transactions).executeWithoutResult(status -> action.run());
  }

  @Test
  void mutationsRequireTheCallerTransaction() {
    var request = request(NOW);
    assertThrows(IllegalTransactionStateException.class, () -> requests.enqueue(request));
    assertThrows(
        IllegalTransactionStateException.class,
        () -> requests.findByIntakeIdForUpdate(request.intakeId()));
    assertThrows(
        IllegalTransactionStateException.class, () -> requests.complete(request.intakeId()));
    assertThrows(IllegalTransactionStateException.class, () -> requests.recordRetry(request, NOW));
  }

  @Test
  void enqueueCommitsAndRollbackLeavesNoIntent() {
    var committed = request(NOW);
    write(() -> requests.enqueue(committed));
    assertEquals(committed, requests.findByIntakeId(committed.intakeId()).orElseThrow());
    var rolledBack = request(NOW);
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              requests.enqueue(rolledBack);
              status.setRollbackOnly();
            });
    assertTrue(requests.findByIntakeId(rolledBack.intakeId()).isEmpty());
    write(() -> requests.complete(committed.intakeId()));
  }

  @Test
  void duplicateEvidenceCannotResetRetryOrRedirectSource() {
    var original = request(NOW);
    write(() -> requests.enqueue(original));
    write(() -> requests.recordRetry(original, NOW.plusSeconds(60)));
    write(() -> requests.enqueue(original));
    var retry = original.retryAt(NOW.plusSeconds(60));
    assertEquals(retry, requests.findByIntakeId(original.intakeId()).orElseThrow());
    var conflicting =
        new PendingInventoryConsumption(
            original.intakeId(),
            UUID.randomUUID().toString(),
            original.olderAdultId(),
            original.confirmedAt(),
            NOW,
            NOW,
            0);
    assertEquals(
        IllegalStateException.class,
        assertThrows(
                InvalidDataAccessApiUsageException.class,
                () -> write(() -> requests.enqueue(conflicting)))
            .getCause()
            .getClass());
    assertEquals(retry, requests.findByIntakeId(original.intakeId()).orElseThrow());
    write(() -> requests.complete(original.intakeId()));
  }

  @Test
  void staleFailureCannotOverwriteNewerRetryOrResurrectCompletedIntent() {
    var original = request(NOW);
    write(() -> requests.enqueue(original));
    write(() -> requests.recordRetry(original, NOW.plusSeconds(60)));
    write(() -> requests.recordRetry(original, NOW.plusSeconds(120)));
    assertEquals(
        original.retryAt(NOW.plusSeconds(60)),
        requests.findByIntakeId(original.intakeId()).orElseThrow());
    write(() -> requests.complete(original.intakeId()));
    write(() -> requests.recordRetry(original, NOW.plusSeconds(180)));
    assertTrue(requests.findByIntakeId(original.intakeId()).isEmpty());
  }

  @Test
  void dueCursorSurvivesDeletionAndEqualTimeRowsWithoutDuplicates() {
    var originals = new ArrayList<PendingInventoryConsumption>();
    for (int i = 0; i < 7; i++) originals.add(request(NOW));
    var later = request(NOW.plusSeconds(60));
    write(
        () -> {
          originals.forEach(requests::enqueue);
          requests.enqueue(later);
        });
    var found = new ArrayList<String>();
    PendingInventoryConsumption cursor = null;
    while (true) {
      var page = requests.findDueAfter(NOW, cursor, 2);
      if (page.isEmpty()) break;
      for (var item : page) {
        found.add(item.intakeId());
        write(() -> requests.complete(item.intakeId()));
      }
      cursor = page.getLast();
    }
    assertEquals(
        originals.stream().map(PendingInventoryConsumption::intakeId).sorted().toList(), found);
    assertFalse(found.contains(later.intakeId()));
    assertEquals(List.of(later), requests.findDueAfter(NOW.plusSeconds(60), null, 100));
    write(() -> requests.complete(later.intakeId()));
    assertEquals(
        IllegalArgumentException.class,
        assertThrows(
                InvalidDataAccessApiUsageException.class, () -> requests.findDueAfter(NOW, null, 0))
            .getCause()
            .getClass());
    assertEquals(
        IllegalArgumentException.class,
        assertThrows(
                InvalidDataAccessApiUsageException.class,
                () -> requests.findDueAfter(NOW, null, 101))
            .getCause()
            .getClass());
  }
}
