package com.tata.familymonitoring.infrastructure.persistence;

import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.time.Instant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates one follow-up (older adult 1, caregiver 1) with one open alert in dev, so the API and
 * Swagger can be tried. In production the monitor will come from the Care Link context and the
 * alerts from Omission & Escalation.
 */
@Component
@Profile("dev")
public class FamilyMonitoringDataSeeder implements CommandLineRunner {

  private static final String SAMPLE_CARE_LINK_ID = "00000000-0000-0000-0000-000000000001";
  private static final String SAMPLE_OLDER_ADULT_ID = "00000000-0000-0000-0000-000000000001";
  private static final String SAMPLE_FAMILIAR_ID = "00000000-0000-0000-0000-000000000001";

  private final IFamilyMonitorRepository repository;

  public FamilyMonitoringDataSeeder(IFamilyMonitorRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (repository.findByOlderAdultId(SAMPLE_OLDER_ADULT_ID).isPresent()) {
      return;
    }
    FamilyMonitor monitor =
        new FamilyMonitor(SAMPLE_CARE_LINK_ID, SAMPLE_OLDER_ADULT_ID, SAMPLE_FAMILIAR_ID);
    Instant now = Instant.now();
    monitor.addAlert(
        "00000000-0000-0000-0000-000000000101",
        "Losartan 50 mg",
        now.minusSeconds(3600),
        "Intake not confirmed within the grace period",
        now.minusSeconds(1800));
    repository.save(monitor);
  }
}
