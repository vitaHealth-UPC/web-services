package com.tata.shared.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Leaves every endpoint open in the dev profile so the API and Swagger UI can be used before
 * authentication exists. Remove it when Identity & Subscription adds the real security chain.
 */
@Configuration
@Profile("dev")
public class DevelopmentSecurityConfiguration {

  @Bean
  public SecurityFilterChain developmentSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .build();
  }
}
