package com.vitahealth.tata.carelink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.carelink.application.internal.queryservices.FindCareLinkByIdQueryHandler;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.queries.FindCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

/** Distinguishes expected lookup absence from failed persistence access. */
class OptionalCareLinkLookupTest {
  private final CareLinkRepository relationships = mock(CareLinkRepository.class);
  private final FindCareLinkByIdQueryHandler handler =
      new FindCareLinkByIdQueryHandler(relationships);

  @Test
  void missingRelationshipIsAnEmptyResult() {
    when(relationships.findById("missing")).thenReturn(Optional.empty());
    assertTrue(handler.handle(new FindCareLinkByIdQuery("missing")).isEmpty());
  }

  @Test
  void existingRelationshipIsMappedWithoutChangingItsLifecycle() {
    var now = Instant.parse("2026-10-13T12:00:00Z");
    var link =
        CareLink.createPending(
            "caregiver", "adult", LinkingCode.issue("TATA-4821", now, Duration.ofMinutes(15)), now);
    when(relationships.findById(link.id())).thenReturn(Optional.of(link));
    var result = handler.handle(new FindCareLinkByIdQuery(link.id())).orElseThrow();
    assertEquals(link.id(), result.id());
    assertEquals(link.status(), result.status());
    assertEquals("adult", result.olderAdultId());
  }

  @Test
  void databaseFailureIsNotTreatedAsMissingRelationship() {
    var failure = new DataAccessResourceFailureException("database unavailable");
    when(relationships.findById("link")).thenThrow(failure);
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> handler.handle(new FindCareLinkByIdQuery("link"))));
  }
}
