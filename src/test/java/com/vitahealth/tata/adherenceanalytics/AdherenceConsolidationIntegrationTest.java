package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vitahealth.tata.adherenceanalytics.application.internal.commandservices.ConsolidateWeeklyPeriodCommandHandler;
import com.vitahealth.tata.adherenceanalytics.application.queryservices.AdherenceSnapshotQueryService;
import com.vitahealth.tata.adherenceanalytics.domain.model.commands.ConsolidateWeeklyPeriodCommand;
import com.vitahealth.tata.adherenceanalytics.domain.model.queries.GetAdherenceSnapshotQuery;
import com.vitahealth.tata.adherenceanalytics.infrastructure.events.SpringAdherenceEventPublisher;
import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.assemblers.AdherenceSnapshotPersistenceAssembler;
import com.vitahealth.tata.adherenceanalytics.infrastructure.persistence.jpa.repositories.AdherenceSnapshotJpaRepository;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.*;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.vitahealth.tata.shared.fixtures.ConsentedAdultHttpFixture;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AdherenceConsolidationIntegrationTest {
  @Autowired ConsolidateWeeklyPeriodCommandHandler handler;
  @Autowired IntakeRepository intakes;
  @Autowired AdherenceSnapshotQueryService historical;
  @Autowired AdherenceSnapshotJpaRepository snapshots;
  @Autowired WebApplicationContext context;
  @MockitoSpyBean SpringAdherenceEventPublisher publisher;
  final Instant from = Instant.parse("2026-10-01T00:00:00Z");
  final Instant to = from.plusSeconds(7 * 86400);

  private void omissions(String owner) {
    var medication = UUID.randomUUID().toString();
    for (int day = 0; day < 3; day++) {
      var intake =
          Intake.createScheduled(
              UUID.randomUUID().toString(),
              medication,
              owner,
              new MedicationSnapshot("Medication", "1 tablet", ""),
              from.plusSeconds(day * 86400L),
              from);
      intake.markOmitted();
      intakes.saveAll(List.of(intake));
    }
  }

  @Test
  void persistedEvidenceSurvivesRetriesAndLaterHistoryChanges() {
    var owner = UUID.randomUUID().toString();
    omissions(owner);
    var first = handler.handle(new ConsolidateWeeklyPeriodCommand(owner, from, to, "UTC"));
    assertEquals(3, first.totalIntakes());
    assertEquals(1, first.patterns().size());
    var confirmed =
        Intake.createScheduled(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            owner,
            new MedicationSnapshot("Medication", "1 tablet", ""),
            from,
            from);
    confirmed.confirm(from, ConfirmationChannel.TOUCH);
    intakes.saveAll(List.of(confirmed));
    var repeat = handler.handle(new ConsolidateWeeklyPeriodCommand(owner, from, to, "Z"));
    assertEquals(first.id(), repeat.id());
    assertEquals(first.consolidatedAt(), repeat.consolidatedAt());
    assertEquals(3, repeat.totalIntakes());
    assertEquals(
        first.patterns(),
        AdherenceSnapshotPersistenceAssembler.toDomain(snapshots.findById(first.id()).orElseThrow())
            .patterns());
    assertTrue(
        historical
            .handle(new GetAdherenceSnapshotQuery(UUID.randomUUID().toString(), first.id()))
            .isEmpty());
  }

  @Test
  void simultaneousConsolidationsKeepOneSnapshot() throws Exception {
    var owner = UUID.randomUUID().toString();
    omissions(owner);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () -> {
                start.await();
                return handler.handle(new ConsolidateWeeklyPeriodCommand(owner, from, to, "UTC"));
              });
      var second =
          executor.submit(
              () -> {
                start.await();
                return handler.handle(new ConsolidateWeeklyPeriodCommand(owner, from, to, "UTC"));
              });
      start.countDown();
      var a = first.get(30, TimeUnit.SECONDS);
      var b = second.get(30, TimeUnit.SECONDS);
      verify(publisher, times(1)).publish(argThat(event -> event.olderAdultId().equals(owner)));
      assertEquals(a.id(), b.id());
      assertEquals(a.consolidatedAt(), b.consolidatedAt());
      assertEquals(
          3,
          AdherenceSnapshotPersistenceAssembler.toDomain(snapshots.findById(a.id()).orElseThrow())
              .totalIntakes());
    }
  }

  @Test
  void publicationFailureRollsBackSnapshotAndAllowsHealthyRetry() {
    var owner = UUID.randomUUID().toString();
    omissions(owner);
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              throw new IllegalStateException("controlled synchronous consumer failure");
            })
        .when(publisher)
        .publish(argThat(event -> event.olderAdultId().equals(owner)));

    assertThrows(
        IllegalStateException.class,
        () -> handler.handle(new ConsolidateWeeklyPeriodCommand(owner, from, to, "UTC")));
    assertTrue(
        snapshots.findAll().stream().noneMatch(snapshot -> snapshot.olderAdultId().equals(owner)));

    doCallRealMethod()
        .when(publisher)
        .publish(argThat(event -> event.olderAdultId().equals(owner)));
    var stored = handler.handle(new ConsolidateWeeklyPeriodCommand(owner, from, to, "UTC"));
    assertEquals(3, stored.omittedIntakes());
    assertTrue(snapshots.existsById(stored.id()));
  }

  @Test
  void endpointReturnsSavedSnapshotAndRejectsOtherOwnerAndInvalidZone() throws Exception {
    var fixture = ConsentedAdultHttpFixture.create(context);
    var mvc = fixture.mvc();
    var owner = fixture.owner();
    mvc.perform(
            post("/api/v1/older-adults/{id}/adherence/consolidations", owner)
                .param("from", from.toString())
                .param("to", to.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalIntakes").value(0));
    var snapshot = handler.handle(new ConsolidateWeeklyPeriodCommand(owner, from, to, "UTC"));
    mvc.perform(
            get(
                "/api/v1/older-adults/{id}/adherence/consolidations/{snapshot}",
                owner,
                snapshot.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(snapshot.id()));
    mvc.perform(
            get(
                "/api/v1/older-adults/{id}/adherence/consolidations/{snapshot}",
                UUID.randomUUID(),
                snapshot.id()))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/older-adults/{id}/adherence/consolidations", owner)
                .param("from", from.toString())
                .param("to", to.toString())
                .param("zone", "invalid-zone"))
        .andExpect(status().isBadRequest());
  }
}
