package com.tata.identitysubscription.infrastructure.persistence;

import com.tata.identitysubscription.application.internal.outboundservices.PasswordHasher;
import com.tata.identitysubscription.domain.model.aggregates.Account;
import com.tata.identitysubscription.domain.model.aggregates.PinCredential;
import com.tata.identitysubscription.domain.model.events.AccountEnabled;
import com.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.tata.identitysubscription.domain.repositories.AccountRepository;
import com.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a verified caregiver account and the PIN of its older adult so the app can be used without the
 * e-mail verification flow, which has no delivery provider yet. Runs only when {@code tata.demo.seed=true}
 * (environment variable {@code TATA_DEMO_SEED}). The matching care link is created by the Care Link context.
 */
@Component
@ConditionalOnProperty(name = "tata.demo.seed", havingValue = "true")
public class DemoAccountSeeder implements CommandLineRunner {
  public static final String CAREGIVER_ID = "d3a10000-0000-4000-8000-000000000001";
  public static final String OLDER_ADULT_ID = "d3a10000-0000-4000-8000-000000000002";

  private final AccountRepository accounts;
  private final PinCredentialRepository pins;
  private final PasswordHasher hasher;
  private final ApplicationEventPublisher events;
  private final String email;
  private final String password;
  private final String pin;

  public DemoAccountSeeder(
      AccountRepository accounts,
      PinCredentialRepository pins,
      PasswordHasher hasher,
      ApplicationEventPublisher events,
      @Value("${tata.demo.email:demo@tata.app}") String email,
      @Value("${tata.demo.password:Tata-Demo-2026}") String password,
      @Value("${tata.demo.pin:1234}") String pin) {
    this.accounts = accounts;
    this.pins = pins;
    this.hasher = hasher;
    this.events = events;
    this.email = email;
    this.password = password;
    this.pin = pin;
  }

  @Override
  @Transactional
  public void run(String... args) {
    var address = new EmailAddress(email);
    if (accounts.findByEmail(address.value()).isEmpty()) {
      accounts.save(
          Account.rehydrate(
              CAREGIVER_ID,
              "Demo Caregiver",
              address,
              hasher.hash(password),
              AccountStatus.ACTIVE,
              null,
              null));
      events.publishEvent(new AccountEnabled(CAREGIVER_ID, Instant.now()));
    }
    if (pins.findByOlderAdultId(OLDER_ADULT_ID).isEmpty()) {
      pins.save(PinCredential.register(OLDER_ADULT_ID, hasher.hash(pin)));
    }
  }
}
