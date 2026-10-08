package com.tata.adherenceanalytics.domain;

import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService.Pattern;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdherencePeriodSnapshotTest {
    private final Instant from = Instant.parse("2026-10-01T00:00:00Z");
    private final Instant to = from.plusSeconds(604800);

    @Test void rejectsInvalidPeriodAndCounts() {
        assertThrows(IllegalArgumentException.class, () -> snapshot(from, -1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> snapshot(to, -1, List.of()));
    }

    @Test void preservesMetricsAndCopiesPatternEvidence() {
        var patterns = new ArrayList<Pattern>();
        patterns.add(new Pattern("med-1", 2, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)));
        var snapshot = snapshot(to, 3, patterns);
        patterns.clear();
        assertEquals(1, snapshot.patterns().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.patterns().clear());
        assertEquals(4, snapshot.confirmedIntakes());
        assertEquals(5, snapshot.totalIntakes());
        assertEquals(80d, snapshot.percentage());
    }

    private AdherencePeriodSnapshot snapshot(Instant end, int onTime, List<Pattern> patterns) {
        return new AdherencePeriodSnapshot("period-1", "adult-1", from, end, "America/Bogota",
                onTime, 1, 1, patterns, 2, to);
    }
}
