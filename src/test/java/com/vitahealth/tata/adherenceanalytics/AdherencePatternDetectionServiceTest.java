package com.vitahealth.tata.adherenceanalytics;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects.AdherenceOmissionObservation;
import com.vitahealth.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdherencePatternDetectionServiceTest {
  final AdherencePatternDetectionService service = new AdherencePatternDetectionService();
  final Instant start = Instant.parse("2026-10-05T04:30:00Z");

  @Test
  void multipleDosesOnOneDayDoNotEstablishRecurrence() {
    assertTrue(
        service
            .detect(
                List.of(
                    new AdherenceOmissionObservation("med", start, true),
                    new AdherenceOmissionObservation("med", start.plusSeconds(60), true),
                    new AdherenceOmissionObservation("med", start.plusSeconds(120), true)),
                ZoneId.of("UTC"),
                3)
            .isEmpty());
  }

  @Test
  void threeOmissionDaysProduceOneDeterministicPatternInRequestedZone() {
    var result =
        service.detect(
            List.of(
                new AdherenceOmissionObservation("med", start, true),
                new AdherenceOmissionObservation("med", start.plusSeconds(86400), true),
                new AdherenceOmissionObservation("med", start.plusSeconds(172800), true),
                new AdherenceOmissionObservation("other", start, false)),
            ZoneId.of("America/Bogota"),
            3);
    assertEquals(1, result.size());
    assertEquals("2026-10-04", result.getFirst().firstDay().toString());
    assertEquals(3, result.getFirst().omissionDays());
  }
}
