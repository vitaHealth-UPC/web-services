package com.vitahealth.tata.intakeexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakePersistenceEntity;
import com.vitahealth.tata.intakeexecution.infrastructure.persistence.jpa.repositories.IntakeJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Verifies database selection agrees with the agenda when treatments share a scheduled instant. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IntakeNextOrderingIntegrationTest {
  private static final Instant AT = Instant.parse("2026-10-15T12:00:00Z");
  @Autowired IntakeJpaRepository storage;
  @Autowired IntakeRepository repository;

  @Test
  void simultaneousPendingSlotsUseSameIdentityOrderAsAgenda() {
    var adult = UUID.randomUUID().toString();
    var first = slot("10000000-0000-0000-0000-000000000001", adult, AT, IntakeStatus.PENDING);
    var second = slot("20000000-0000-0000-0000-000000000002", adult, AT, IntakeStatus.PENDING);
    storage.saveAllAndFlush(List.of(second, first));

    var next = repository.findNextPendingByOlderAdultId(adult, AT).orElseThrow();
    var agenda = repository.findAgenda(adult, AT, AT.plusSeconds(1));
    assertEquals(List.of(first.getId(), second.getId()), agenda.stream().map(i -> i.id()).toList());
    assertEquals(agenda.getFirst().id(), next.id());
    storage.deleteById(first.getId());
    storage.flush();
    assertEquals(
        second.getId(), repository.findNextPendingByOlderAdultId(adult, AT).orElseThrow().id());
  }

  @Test
  void lowerBoundOwnerAndPendingStatusRemainRequired() {
    var adult = UUID.randomUUID().toString();
    var confirmed = slot(UUID.randomUUID().toString(), adult, AT, IntakeStatus.CONFIRMED);
    confirmed.setConfirmation(AT, ConfirmationChannel.TOUCH);
    var eligible =
        slot(UUID.randomUUID().toString(), adult, AT.plusSeconds(60), IntakeStatus.PENDING);
    storage.saveAllAndFlush(
        List.of(
            confirmed,
            slot(UUID.randomUUID().toString(), adult, AT.minusSeconds(1), IntakeStatus.PENDING),
            slot(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                AT,
                IntakeStatus.PENDING),
            eligible));
    assertEquals(
        eligible.getId(), repository.findNextPendingByOlderAdultId(adult, AT).orElseThrow().id());
    assertTrue(repository.findNextPendingByOlderAdultId(adult, AT.plusSeconds(61)).isEmpty());
  }

  private static IntakePersistenceEntity slot(
      String id, String adult, Instant at, IntakeStatus status) {
    return new IntakePersistenceEntity(
        id,
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        adult,
        "Losartán",
        "1 comprimido",
        "",
        at,
        status,
        AT.minusSeconds(120));
  }
}
