package com.vitahealth.tata.omissionescalation;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.entities.EscalationRecord;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Reads real stored histories without silently repairing contradictory evidence. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StoredEscalationHistoryIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
  @Autowired IOmissionCaseRepository repository;
  @Autowired JdbcTemplate jdbc;
  @Autowired EntityManager entities;

  private OmissionCase stored() {
    var value =
        new OmissionCase(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "Losartan",
            NOW.minusSeconds(3600),
            GracePeriod.startingAt(NOW.minusSeconds(3600), Duration.ofMinutes(30)));
    value.markOmitted(NOW);
    value.escalate("first", NOW.plusSeconds(60));
    value.escalate("second", NOW.plusSeconds(120));
    return repository.save(value);
  }

  @Test
  void managedSavePreservesCreationTimeAndRejectsRemovedHistory() {
    var source = stored();
    var createdAt = source.getCreatedAt();
    var forged = copy(source, source.getStatus(), source.getEscalations(), Instant.EPOCH);
    forged.close(NOW.plusSeconds(180));
    var saved = repository.save(forged);
    assertEquals(createdAt, saved.getCreatedAt());
    assertEquals(
        source.getEscalations().getFirst().getId(), saved.getEscalations().getFirst().getId());
    var before =
        jdbc.queryForList(
            "select * from escalation_records where omission_case_id = ? order by id",
            source.getId());
    var omittedHistory = copy(source, OmissionCaseStatus.OMITTED, List.of(), createdAt);
    assertThrows(RuntimeException.class, () -> repository.save(omittedHistory));
    entities.clear();
    assertEquals(
        before,
        jdbc.queryForList(
            "select * from escalation_records where omission_case_id = ? order by id",
            source.getId()));
    assertEquals(
        "CLOSED",
        jdbc.queryForObject(
            "select status from omission_cases where id = ?", String.class, source.getId()));
  }

  @Test
  void deliveryOnlySaveUpdatesParentAuditWithoutChangingCreationEvidence() {
    var source = stored();
    source.addAlert(NOW.plusSeconds(130));
    source.markAlertFailed("push provider failure");
    source = repository.save(source);
    var id = source.getId();
    var originalCreation = source.getCreatedAt();
    jdbc.update(
        "update omission_cases set updated_at = ? where id = ?", Timestamp.from(Instant.EPOCH), id);
    entities.clear();
    var fresh = repository.findById(id).orElseThrow();
    fresh.markAlertSent(NOW.plusSeconds(180));
    var saved = repository.save(fresh);
    assertEquals(originalCreation, saved.getCreatedAt());
    assertTrue(saved.getUpdatedAt().isAfter(Instant.EPOCH));
    var updated =
        jdbc.queryForObject(
            "select updated_at from omission_cases where id = ?", Timestamp.class, id);
    entities.clear();
    repository.findById(id).orElseThrow();
    entities.flush();
    assertEquals(
        updated,
        jdbc.queryForObject(
            "select updated_at from omission_cases where id = ?", Timestamp.class, id));
  }

  @Test
  void staleEarlierSnapshotCannotReopenClosedCase() {
    var source = stored();
    var stale = copy(source, source.getStatus(), source.getEscalations(), source.getCreatedAt());
    source.close(NOW.plusSeconds(180));
    repository.save(source);
    assertThrows(RuntimeException.class, () -> repository.save(stale));
    assertEquals(
        "CLOSED",
        jdbc.queryForObject(
            "select status from omission_cases where id = ?", String.class, source.getId()));
    assertEquals(
        Timestamp.from(NOW.plusSeconds(180)),
        jdbc.queryForObject(
            "select closed_at from omission_cases where id = ?", Timestamp.class, source.getId()));
  }

  private static OmissionCase copy(
      OmissionCase source,
      OmissionCaseStatus status,
      List<EscalationRecord> history,
      Instant createdAt) {
    return OmissionCase.rehydrate(
        source.getId(),
        source.getIntakeId(),
        source.getOlderAdultId(),
        source.getMedicationName(),
        source.getScheduledAt(),
        status,
        source.getGracePeriod(),
        source.getReinforcedReminderSentAt(),
        source.getOmittedAt(),
        null,
        source.getAlerts(),
        history,
        createdAt,
        source.getUpdatedAt());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void reservedReadRequiresTheCallerDecisionTransaction() {
    assertThrows(IllegalTransactionStateException.class, () -> repository.findByIdForUpdate(1L));
  }

  @Test
  void reservedReadRefreshesPreviouslyManagedEvidence() {
    var value = stored();
    var earlier = repository.findById(value.getId()).orElseThrow();
    assertNull(earlier.getReinforcedReminderSentAt());
    jdbc.update(
        "update omission_cases set reinforced_reminder_sent_at = ? where id = ?",
        Timestamp.from(NOW.minusSeconds(30)),
        value.getId());
    var refreshed = repository.findByIdForUpdate(value.getId()).orElseThrow();
    assertEquals(NOW.minusSeconds(30), refreshed.getReinforcedReminderSentAt());
    assertEquals(2, refreshed.currentLevel().value());
    assertEquals(
        value.getEscalations().getLast().getId(), refreshed.getEscalations().getLast().getId());
  }

  @Test
  void validHistoryStoredAgainstReversePhysicalIdsLoadsWithoutRewritingRows() {
    var value = stored();
    var first = value.getEscalations().getFirst().getId();
    var second = value.getEscalations().getLast().getId();
    jdbc.update(
        "update escalation_records set level = ?, triggered_at = ? where id = ?",
        2,
        Timestamp.from(NOW.plusSeconds(120)),
        first);
    jdbc.update(
        "update escalation_records set level = ?, triggered_at = ? where id = ?",
        1,
        Timestamp.from(NOW.plusSeconds(60)),
        second);
    var before =
        jdbc.queryForList(
            "select * from escalation_records where omission_case_id = ? order by id",
            value.getId());
    entities.clear();
    var loaded = repository.findById(value.getId()).orElseThrow();
    assertEquals(second, loaded.getEscalations().getFirst().getId());
    assertEquals(first, loaded.getEscalations().getLast().getId());
    assertEquals(2, loaded.currentLevel().value());
    assertEquals(NOW.plusSeconds(120), loaded.lastActivityAt());
    assertEquals(
        before,
        jdbc.queryForList(
            "select * from escalation_records where omission_case_id = ? order by id",
            value.getId()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing-omission",
        "premature-omission",
        "active-closed",
        "closed-missing-time",
        "closed-before-history",
        "pending-with-history",
        "omitted-with-history"
      })
  void corruptLifecycleIsRejectedWithoutRewritingParentOrHistory(String kind) {
    var value = stored();
    switch (kind) {
      case "missing-omission" ->
          jdbc.update("update omission_cases set omitted_at = null where id = ?", value.getId());
      case "premature-omission" ->
          jdbc.update(
              "update omission_cases set omitted_at = ? where id = ?",
              Timestamp.from(NOW.minusSeconds(3600)),
              value.getId());
      case "active-closed" ->
          jdbc.update(
              "update omission_cases set closed_at = ? where id = ?",
              Timestamp.from(NOW.plusSeconds(180)),
              value.getId());
      case "closed-missing-time" ->
          jdbc.update("update omission_cases set status = 'CLOSED' where id = ?", value.getId());
      case "closed-before-history" ->
          jdbc.update(
              "update omission_cases set status = 'CLOSED', closed_at = ? where id = ?",
              Timestamp.from(NOW),
              value.getId());
      case "pending-with-history" ->
          jdbc.update("update omission_cases set status = 'PENDING' where id = ?", value.getId());
      case "omitted-with-history" ->
          jdbc.update("update omission_cases set status = 'OMITTED' where id = ?", value.getId());
      default -> throw new IllegalArgumentException(kind);
    }
    var parent = jdbc.queryForMap("select * from omission_cases where id = ?", value.getId());
    var children =
        jdbc.queryForList(
            "select * from escalation_records where omission_case_id = ? order by id",
            value.getId());
    entities.clear();
    var failure = assertThrows(RuntimeException.class, () -> repository.findById(value.getId()));
    boolean technical = false;
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof IllegalStateException
          && "stored omission case is inconsistent".equals(cause.getMessage())) technical = true;
    }
    assertTrue(technical);
    assertEquals(
        parent, jdbc.queryForMap("select * from omission_cases where id = ?", value.getId()));
    assertEquals(
        children,
        jdbc.queryForList(
            "select * from escalation_records where omission_case_id = ? order by id",
            value.getId()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"duplicate", "gap", "reversed", "zero", "blank"})
  void corruptHistoryRaisesTechnicalFailureAndRetainsExactRows(String kind) {
    var value = stored();
    var first = value.getEscalations().getFirst().getId();
    var second = value.getEscalations().getLast().getId();
    switch (kind) {
      case "duplicate" ->
          jdbc.update("update escalation_records set level = 1 where id = ?", second);
      case "gap" -> jdbc.update("update escalation_records set level = 3 where id = ?", second);
      case "reversed" ->
          jdbc.update(
              "update escalation_records set triggered_at = ? where id = ?",
              Timestamp.from(NOW),
              second);
      case "zero" -> jdbc.update("update escalation_records set level = 0 where id = ?", first);
      case "blank" -> jdbc.update("update escalation_records set reason = ' ' where id = ?", first);
      default -> throw new IllegalArgumentException(kind);
    }
    var before =
        jdbc.queryForList(
            "select * from escalation_records where omission_case_id = ? order by id",
            value.getId());
    var parent = jdbc.queryForMap("select * from omission_cases where id = ?", value.getId());
    entities.clear();
    var failure = assertThrows(RuntimeException.class, () -> repository.findById(value.getId()));
    Throwable origin = failure;
    boolean technical = false;
    while (origin != null) {
      if (origin instanceof IllegalStateException
          && "stored omission case is inconsistent".equals(origin.getMessage())) technical = true;
      origin = origin.getCause();
    }
    assertTrue(technical);
    assertEquals(
        before,
        jdbc.queryForList(
            "select * from escalation_records where omission_case_id = ? order by id",
            value.getId()));
    assertEquals(
        parent, jdbc.queryForMap("select * from omission_cases where id = ?", value.getId()));
  }
}
