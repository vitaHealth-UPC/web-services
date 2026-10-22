package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.adherenceanalytics.application.exceptions.AdherenceSourceUnavailableException;
import com.vitahealth.tata.adherenceanalytics.application.exceptions.InvalidAdherenceSourceException;
import com.vitahealth.tata.adherenceanalytics.infrastructure.acl.IntakeEvidenceReader;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Checks source ownership, half-open periods and failure classification before analytics mapping.
 */
class IntakeEvidenceReaderTest {
  private final IntakeContextFacade source = mock(IntakeContextFacade.class);
  private final IntakeEvidenceReader reader = new IntakeEvidenceReader(source);
  private final Instant from = Instant.parse("2026-10-01T00:00:00Z");
  private final Instant to = from.plusSeconds(86400);

  private IntakeResult record(String id, String owner, Instant scheduled) {
    return new IntakeResult(
        id,
        "treatment",
        "medication",
        owner,
        "Losartan",
        "50 mg",
        "",
        scheduled,
        IntakeStatus.OMITTED,
        null,
        null,
        false);
  }

  @Test
  void acceptsInclusiveStartAndReturnsImmutableEvidence() {
    var evidence = record("intake", "adult", from);
    when(source.findEvidence("adult", from, to)).thenReturn(List.of(evidence));
    var found = reader.find("adult", from, to);
    assertEquals(List.of(evidence), found);
    assertThrows(UnsupportedOperationException.class, found::clear);
  }

  @Test
  void rejectsForeignOwnerAndBothOutsidePeriodBounds() {
    for (var invalid :
        List.of(
            record("one", "foreign", from),
            record("two", "adult", from.minusNanos(1)),
            record("three", "adult", to))) {
      when(source.findEvidence("adult", from, to)).thenReturn(List.of(invalid));
      assertThrows(InvalidAdherenceSourceException.class, () -> reader.find("adult", from, to));
    }
  }

  @Test
  void rejectsDuplicateIdentityAndMissingEvidence() {
    var evidence = record("intake", "adult", from);
    when(source.findEvidence("adult", from, to)).thenReturn(List.of(evidence, evidence));
    assertThrows(InvalidAdherenceSourceException.class, () -> reader.find("adult", from, to));
    when(source.findEvidence("adult", from, to)).thenReturn(null);
    assertThrows(InvalidAdherenceSourceException.class, () -> reader.find("adult", from, to));
  }

  @Test
  void classifiesProviderFailuresAndRetainsTheirOriginalCause() {
    var failure = new IllegalArgumentException("provider failure");
    when(source.findEvidence("adult", from, to)).thenThrow(failure);
    assertSame(
        failure,
        assertThrows(
                AdherenceSourceUnavailableException.class, () -> reader.find("adult", from, to))
            .getCause());
  }

  @Test
  void rejectsConfirmationEvidenceThatContradictsTheRecordedStatus() {
    for (var status : IntakeStatus.values()) {
      boolean confirmed = status == IntakeStatus.CONFIRMED || status == IntakeStatus.LATE;
      var invalid =
          new IntakeResult(
              "intake",
              "treatment",
              "medication",
              "adult",
              "Losartan",
              "50 mg",
              "",
              from,
              status,
              confirmed ? null : from,
              null,
              false);
      when(source.findEvidence("adult", from, to)).thenReturn(List.of(invalid));
      assertThrows(InvalidAdherenceSourceException.class, () -> reader.find("adult", from, to));
    }
  }
}
