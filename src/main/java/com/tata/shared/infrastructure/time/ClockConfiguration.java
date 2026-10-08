package com.tata.shared.infrastructure.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supplies the UTC clock used by application services without coupling domain models to Spring. */
@Configuration
public class ClockConfiguration {
    /** Returns the production time source; tests can supply a fixed clock. */
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }
}
