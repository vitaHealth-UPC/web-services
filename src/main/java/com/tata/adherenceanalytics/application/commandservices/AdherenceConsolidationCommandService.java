package com.tata.adherenceanalytics.application.commandservices;
import com.tata.adherenceanalytics.domain.model.AdherencePeriodSnapshot;
import java.time.Instant;
import java.util.Optional;
public interface AdherenceConsolidationCommandService {
 AdherencePeriodSnapshot handle(String owner, Instant from, Instant to, String zone);
 Optional<AdherencePeriodSnapshot> find(String owner, String id);
}
