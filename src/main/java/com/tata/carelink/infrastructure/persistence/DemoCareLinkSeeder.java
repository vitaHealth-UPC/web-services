package com.tata.carelink.infrastructure.persistence;

import com.tata.carelink.domain.model.aggregates.CareLink;
import com.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.tata.carelink.domain.model.events.CareLinkConfirmed;
import com.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.tata.carelink.domain.model.valueobjects.Consent;
import com.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.tata.carelink.domain.repositories.CareLinkRepository;
import com.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the older adult and the confirmed care link of the demo caregiver (see the demo account seeder in
 * Identity &amp; Subscription). Runs only when {@code tata.demo.seed=true}. The ids are duplicated on purpose:
 * contexts do not import each other's implementation.
 */
@Component
@ConditionalOnProperty(name = "tata.demo.seed", havingValue = "true")
public class DemoCareLinkSeeder implements CommandLineRunner {
  private static final String CAREGIVER_ID = "d3a10000-0000-4000-8000-000000000001";
  private static final String OLDER_ADULT_ID = "d3a10000-0000-4000-8000-000000000002";
  private static final String CARE_LINK_ID = "d3a10000-0000-4000-8000-000000000003";

  private final OlderAdultProfileRepository profiles;
  private final CareLinkRepository careLinks;
  private final ApplicationEventPublisher events;

  public DemoCareLinkSeeder(
      OlderAdultProfileRepository profiles,
      CareLinkRepository careLinks,
      ApplicationEventPublisher events) {
    this.profiles = profiles;
    this.careLinks = careLinks;
    this.events = events;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (careLinks.findConfirmed(CAREGIVER_ID, OLDER_ADULT_ID).isPresent()) {
      return;
    }
    var now = Instant.now();
    profiles.save(
        OlderAdultProfile.rehydrate(
            OLDER_ADULT_ID,
            CAREGIVER_ID,
            new OlderAdultBasicData("Rosa Vargas", LocalDate.of(1948, 5, 12)),
            new EmergencyContact("Diego Vargas", "Son", "999888777"),
            now,
            now));
    careLinks.save(
        CareLink.rehydrate(
            CARE_LINK_ID,
            CAREGIVER_ID,
            OLDER_ADULT_ID,
            CareLinkStatus.CONFIRMED,
            LinkingCode.rehydrate("DEMOLINK", now.plusSeconds(3600), now),
            Consent.rehydrate(true, now),
            now,
            now,
            now));
    events.publishEvent(new CareLinkConfirmed(CARE_LINK_ID, CAREGIVER_ID, OLDER_ADULT_ID));
  }
}
