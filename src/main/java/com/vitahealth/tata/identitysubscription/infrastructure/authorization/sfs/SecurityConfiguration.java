package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import com.vitahealth.tata.identitysubscription.application.queryservices.SessionQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Configures stateless bearer authentication before MVC resource ownership checks. */
@Configuration
public class SecurityConfiguration {
  /** Disables browser login/session mechanisms and installs the application credential filter. */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, SessionQueryService sessions)
      throws Exception {
    http.csrf(c -> c.disable())
        .cors(Customizer.withDefaults())
        .httpBasic(c -> c.disable())
        .formLogin(c -> c.disable())
        .logout(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(PublicSessionEndpoints::matches)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(
            new SessionAuthenticationFilter(sessions), UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
