package com.tata.adherenceanalytics.interfaces.rest.transform;
import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import com.tata.adherenceanalytics.domain.services.AdherencePatternDetectionService.Pattern;
import java.time.Instant;
import java.util.List;
import com.tata.adherenceanalytics.interfaces.rest.resources.AdherenceSnapshotResource;
public final class AdherenceSnapshotResourceAssembler {
 private AdherenceSnapshotResourceAssembler() {}
    public static AdherenceSnapshotResource from(AdherencePeriodSnapshot snapshot) {
        return new AdherenceSnapshotResource(snapshot.id(), snapshot.olderAdultId(), snapshot.from(), snapshot.to(),
                snapshot.zone(), snapshot.consolidatedAt(), snapshot.confirmedIntakes(), snapshot.totalIntakes(),
                snapshot.percentage(), snapshot.onTimeIntakes(), snapshot.lateIntakes(), snapshot.omittedIntakes(),
                snapshot.patterns(), snapshot.minimumOmissionDays());
    }
}
