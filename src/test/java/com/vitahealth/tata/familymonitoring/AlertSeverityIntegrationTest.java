package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.familymonitoring.application.internal.queryservices.AlertResultProjection;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertSeverity;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.familymonitoring.interfaces.rest.transform.AlertSummaryResourceFromResultAssembler;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.interfaces.acl.OmissionContextFacade;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Exercises the real cross-context query, scalar JPA aggregate and response projection. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AlertSeverityIntegrationTest {
  private static final Instant AT = Instant.parse("2026-11-18T08:00:00Z");
  @Autowired IOmissionCaseRepository omissions;
  @Autowired AlertResultProjection projection;
  @Autowired OmissionContextFacade facade;
  @Autowired EntityManager entities;

  private OmissionCase store(String adult, int level) {
    var value =
        new OmissionCase(
            UUID.randomUUID().toString(),
            adult,
            "Medication",
            AT,
            GracePeriod.startingAt(AT, Duration.ofMinutes(30)));
    value.markOmitted(AT.plusSeconds(1800));
    for (int step = 1; step <= level; step++) {
      value.escalate("Recorded follow-up", AT.plusSeconds(1800L * (step + 1)));
    }
    return omissions.save(value);
  }

  private AlertSummary alert(long id, String intake) {
    return AlertSummary.rehydrate(
        id,
        intake,
        "Medication",
        AT,
        "No confirmation",
        AlertStatus.OPEN,
        AT.plusSeconds(1800),
        null);
  }

  @Test
  void classifiesRecordedLevelsAndKeepsMissingEvidenceUnknown() {
    var adult = UUID.randomUUID().toString();
    var normal = store(adult, 0);
    var first = store(adult, 1);
    var second = store(adult, 2);
    var critical = store(adult, 3);
    entities.flush();
    entities.clear();
    var result =
        projection.project(
            adult,
            List.of(
                alert(1, normal.getIntakeId()),
                alert(2, first.getIntakeId()),
                alert(3, second.getIntakeId()),
                alert(4, critical.getIntakeId()),
                alert(5, "missing")));
    assertEquals(
        List.of(
            AlertSeverity.STANDARD,
            AlertSeverity.HIGH,
            AlertSeverity.HIGH,
            AlertSeverity.CRITICAL,
            AlertSeverity.UNKNOWN),
        result.stream().map(value -> value.severity()).toList());
    assertEquals(
        AlertSeverity.CRITICAL,
        AlertSummaryResourceFromResultAssembler.toResourceFromResult(result.get(3)).severity());
    assertThrows(UnsupportedOperationException.class, () -> result.clear());
  }

  @Test
  void anotherAdultsCaseCannotSupplyPriorityToThisFollowUp() {
    var stored = store("another-adult", 3);
    assertEquals(
        AlertSeverity.UNKNOWN,
        projection.project("selected-adult", alert(1, stored.getIntakeId())).severity());
    assertEquals(0, facade.escalationLevels("selected-adult", Set.of(stored.getIntakeId())).size());
  }

  @Test
  void newEscalationAppearsWithoutRewritingTheFamilyAlertOrEarlierReceipt() {
    var adult = UUID.randomUUID().toString();
    var source = store(adult, 0);
    var followUp = alert(1, source.getIntakeId());
    var before = projection.project(adult, followUp);
    var reserved = omissions.findByIdForUpdate(source.getId()).orElseThrow();
    reserved.escalate("one", AT.plusSeconds(3600));
    reserved.escalate("two", AT.plusSeconds(5400));
    reserved.escalate("three", AT.plusSeconds(7200));
    omissions.save(reserved);
    entities.flush();
    entities.clear();
    assertEquals(AlertSeverity.STANDARD, before.severity());
    assertEquals(AlertSeverity.CRITICAL, projection.project(adult, followUp).severity());
    followUp.markAttended();
    assertEquals(AlertSeverity.CRITICAL, projection.project(adult, followUp).severity());
    followUp.close(AT.plusSeconds(7300));
    var closed = projection.project(adult, followUp);
    assertEquals(AlertStatus.CLOSED, closed.status());
    assertEquals(AlertSeverity.CRITICAL, closed.severity());
  }

  @Test
  void noAlertsNeedsNoSourceEvidenceAndInvalidLevelsAreRejected() {
    assertEquals(List.of(), projection.project("adult", List.of()));
    assertEquals(0, facade.escalationLevels("adult", Set.of()).size());
    assertThrows(IllegalArgumentException.class, () -> AlertSeverity.fromEscalationLevel(4));
    assertThrows(IllegalArgumentException.class, () -> AlertSeverity.fromEscalationLevel(-1));
  }
}
