package com.vitahealth.tata.intakeexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.intakeexecution.application.queryservices.GetIntakeDetailQueryService;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetIntakeDetailQuery;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakePersistenceEntity;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.repositories.IntakeJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Stored corruption remains a technical failure, without conversion to request validation. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IntakeStoredEvidenceQueryIntegrationTest {
  @Autowired IntakeJpaRepository storage;
  @Autowired GetIntakeDetailQueryService detail;
  @Autowired EntityManager entities;

  @Test
  void incompleteStoredConfirmationPropagatesTechnicalFailureWithoutRepair() {
    var id = UUID.randomUUID().toString();
    storage.saveAndFlush(
        new IntakePersistenceEntity(
            id,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Losartán",
            "1 comprimido",
            "",
            Instant.EPOCH,
            IntakeStatus.CONFIRMED,
            Instant.EPOCH));
    entities.clear();

    var failure =
        assertThrows(RuntimeException.class, () -> detail.handle(new GetIntakeDetailQuery(id)));
    Throwable origin = failure;
    while (origin.getCause() != null && !(origin instanceof IllegalStateException)) {
      origin = origin.getCause();
    }
    assertInstanceOf(IllegalStateException.class, origin);
    assertEquals("stored intake evidence is inconsistent", origin.getMessage());

    entities.clear();
    var stored = storage.findById(id).orElseThrow();
    assertEquals(IntakeStatus.CONFIRMED, stored.getStatus());
    assertNull(stored.getConfirmedAt());
    assertNull(stored.getConfirmationChannel());
  }
}
