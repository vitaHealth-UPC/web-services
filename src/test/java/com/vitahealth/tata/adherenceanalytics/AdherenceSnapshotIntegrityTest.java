package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.adherenceanalytics.domain.exceptions.AdherenceSnapshotAlreadyExistsException;
import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.adapters.AdherenceSnapshotRepositoryAdapter;
import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.repositories.AdherenceSnapshotJpaRepository;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

/** Verifies snapshot collision recovery never hides unrelated provider failures. */
class AdherenceSnapshotIntegrityTest {
  private static final String PARENT_INSERT =
      "insert into adherence_period_snapshots (id) values (?)";
  private final EntityManager entities = mock(EntityManager.class);
  private final AdherenceSnapshotRepositoryAdapter adapter =
      new AdherenceSnapshotRepositoryAdapter(mock(AdherenceSnapshotJpaRepository.class), entities);

  private AdherencePeriodSnapshot snapshot() {
    var from = Instant.parse("2026-10-01T00:00:00Z");
    return new AdherencePeriodSnapshot(
        "snapshot", "adult", from, from.plusSeconds(604800), "UTC", 0, 0, 0, List.of(), 3, from);
  }

  private RuntimeException violation(String constraint, String state, String sql) {
    return new ConstraintViolationException(
        "private provider details", new SQLException("private", state), sql, constraint);
  }

  @Test
  void translatesOnlyExpectedPrimaryIdentityCollisionsAndRetainsCause() {
    for (var failure :
        List.of(
            violation("adherence_period_snapshots_pkey", "23505", PARENT_INSERT),
            violation("PUBLIC.CONSTRAINT_9 INDEX PUBLIC.PRIMARY_KEY_9", "23505", PARENT_INSERT))) {
      doThrow(failure).when(entities).flush();
      var translated =
          assertThrows(
              AdherenceSnapshotAlreadyExistsException.class, () -> adapter.insert(snapshot()));
      assertSame(failure, translated.getCause());
    }
  }

  @Test
  void preservesUnrelatedConstraintsForeignKeysChildInsertsAndOrdinaryFailures() {
    for (var failure :
        List.of(
            violation("other_unique", "23505", PARENT_INSERT),
            violation("adherence_period_snapshots_pkey", "23503", PARENT_INSERT),
            violation(
                "PUBLIC.PRIMARY_KEY_9",
                "23505",
                "insert into adherence_snapshot_patterns (snapshot_id) values (?)"),
            violation("PUBLIC.PRIMARY_KEY_9", "23505", null),
            new IllegalStateException("provider unavailable"))) {
      doThrow(failure).when(entities).flush();
      assertSame(failure, assertThrows(RuntimeException.class, () -> adapter.insert(snapshot())));
    }
  }
}
