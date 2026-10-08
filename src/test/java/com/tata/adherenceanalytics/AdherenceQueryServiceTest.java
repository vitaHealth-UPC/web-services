package com.tata.adherenceanalytics;
import com.tata.adherenceanalytics.application.models.IntakeOutcome;

import com.tata.adherenceanalytics.application.queryservices.AdherenceQueryService;
import com.tata.adherenceanalytics.application.internal.outboundservices.IntakeOutcomePort;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdherenceQueryServiceTest {
    private final Instant from = Instant.parse("2026-10-05T00:00:00Z");
    private AdherenceQueryService service(com.tata.adherenceanalytics.application.models.IntakeOutcome.Status... statuses) {
        return new com.tata.adherenceanalytics.application.internal.queryservices.AdherenceQueryServiceImpl((owner, start, end) -> java.util.Arrays.stream(statuses)
                .map(status -> new IntakeOutcome("medication", from, status)).toList(), 3);
    }
    @Test void timelyAndLateConfirmationsProduceFullAdherence() {
        var metrics = service(com.tata.adherenceanalytics.application.models.IntakeOutcome.Status.CONFIRMED, com.tata.adherenceanalytics.application.models.IntakeOutcome.Status.LATE)
                .weekly("owner", from, from.plusSeconds(86400));
        assertEquals(100d, metrics.percentage());
        assertEquals(2, metrics.confirmedIntakes());
        assertEquals(1, metrics.onTimeIntakes());
        assertEquals(1, metrics.lateIntakes());
        assertEquals(0, metrics.omittedIntakes());
    }
    @Test void omittedOnlyProducesZeroAndPendingOnlyHasInsufficientEvidence() {
        var omitted = service(com.tata.adherenceanalytics.application.models.IntakeOutcome.Status.OMITTED).weekly("owner", from, from.plusSeconds(86400));
        assertEquals(0d, omitted.percentage());
        assertEquals(1, omitted.totalIntakes());
        var pending = service(com.tata.adherenceanalytics.application.models.IntakeOutcome.Status.PENDING).weekly("owner", from, from.plusSeconds(86400));
        assertEquals(0, pending.totalIntakes());
        assertEquals(List.of(), service(com.tata.adherenceanalytics.application.models.IntakeOutcome.Status.PENDING)
                .recommendations("owner", from, from.plusSeconds(86400), "UTC"));
    }
}
