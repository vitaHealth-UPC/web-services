package com.vitahealth.tata.carelink.infrastructure.persistence;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.vitahealth.tata.carelink.domain.model.events.CareLinkConfirmed;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.model.valueobjects.Consent;
import com.vitahealth.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the older adult and the confirmed care link of the demo caregiver (see the demo account
 * seeder in Identity &amp; Subscription). Runs in development/test when {@code
 * tata.demo.seed=true}. The ids are duplicated on purpose: contexts do not import each other's
 * implementation.
 */
@Component
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "tata.demo.seed", havingValue = "true")
public class DemoCareLinkSeeder implements CommandLineRunner {
  private static final String CAREGIVER_ID = "d3a10000-0000-4000-8000-000000000001";
  private static final String OLDER_ADULT_ID = "d3a10000-0000-4000-8000-000000000002";
  private static final String CARE_LINK_ID = "d3a10000-0000-4000-8000-000000000003";

  private final OlderAdultProfileRepository profiles;
  private final CareLinkRepository careLinks;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /** Injects demo persistence, transaction events and the application clock. */
  public DemoCareLinkSeeder(
      OlderAdultProfileRepository profiles,
      CareLinkRepository careLinks,
      ApplicationEventPublisher events,
      Clock clock) {
    this.profiles = profiles;
    this.careLinks = careLinks;
    this.events = events;
    this.clock = clock;
  }

  /** Seeds only absent evidence, preserving existing decisions and profile history. */
  @Override
  @Transactional
  public void run(String... args) {
    if (careLinks.findById(CARE_LINK_ID).isPresent()) {
      return;
    }
    var now = clock.instant();
    if (profiles.findById(OLDER_ADULT_ID).isEmpty())
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
