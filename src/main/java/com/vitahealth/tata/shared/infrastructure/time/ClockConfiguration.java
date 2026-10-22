package com.vitahealth.tata.shared.infrastructure.time;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supplies the UTC clock used by application services without coupling domain models to Spring. */
@Configuration
public class ClockConfiguration {
  /** Returns UTC occurrence times aligned with the database's microsecond timestamp precision. */
  @Bean
  public Clock applicationClock() {
    return withDatabasePrecision(Clock.systemUTC());
  }

  /** Aligns an injected time source before it supplies timestamps to application transitions. */
  public static Clock withDatabasePrecision(Clock source) {
    return Clock.tick(source, Duration.ofNanos(1_000));
  }
}
