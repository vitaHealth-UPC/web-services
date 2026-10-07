package com.tata.adherenceanalytics.interfaces.rest;

import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService.Pattern;
import java.time.Instant;
import java.util.List;

public record AdherenceSnapshotResource(String id, String olderAdultId, Instant from, Instant to,
        String zone, Instant consolidatedAt, int confirmedIntakes, int totalIntakes, double percentage,
        int onTimeIntakes, int lateIntakes, int omittedIntakes, List<Pattern> patterns, int minimumOmissionDays) {
    public static AdherenceSnapshotResource from(AdherencePeriodSnapshot snapshot) {
        return new AdherenceSnapshotResource(snapshot.id(), snapshot.olderAdultId(), snapshot.from(), snapshot.to(),
                snapshot.zone(), snapshot.consolidatedAt(), snapshot.confirmedIntakes(), snapshot.totalIntakes(),
                snapshot.percentage(), snapshot.onTimeIntakes(), snapshot.lateIntakes(), snapshot.omittedIntakes(),
                snapshot.patterns(), snapshot.minimumOmissionDays());
    }
}
