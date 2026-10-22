package com.vitahealth.tata.shared.infrastructure.persistence.jpa.configuration;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Configures technical JPA audit timestamps using the application's injectable clock. */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "persistenceDateTimeProvider")
public class PersistenceAuditingConfiguration {
  /** Supplies the same observable instant used by application operations. */
  @Bean
  public DateTimeProvider persistenceDateTimeProvider(Clock clock) {
    return () -> Optional.of(clock.instant());
  }
}
