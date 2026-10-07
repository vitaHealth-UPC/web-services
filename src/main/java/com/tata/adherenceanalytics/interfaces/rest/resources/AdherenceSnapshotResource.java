package com.tata.adherenceanalytics.interfaces.rest.resources;

import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService.Pattern;
import java.time.Instant;
import java.util.List;

public record AdherenceSnapshotResource(String id, String olderAdultId, Instant from, Instant to,
        String zone, Instant consolidatedAt, int confirmedIntakes, int totalIntakes, double percentage,
        int onTimeIntakes, int lateIntakes, int omittedIntakes, List<Pattern> patterns, int minimumOmissionDays) {

}
