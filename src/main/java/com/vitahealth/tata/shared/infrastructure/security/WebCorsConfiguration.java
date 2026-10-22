package com.vitahealth.tata.shared.infrastructure.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Configures browser origins consistently for public and credentialed requests. */
@Configuration
public class WebCorsConfiguration {
  /** Wildcard access never permits browser credentials; explicit origins may use credentials. */
  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${tata.cors.allowed-origins:*}") String allowedOrigins) {
    var configuration = new CorsConfiguration();
    List<String> origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();
    boolean wildcard = origins.contains("*");
    if (wildcard) {
      configuration.setAllowedOriginPatterns(List.of("*"));
    } else {
      configuration.setAllowedOrigins(origins);
    }
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Authorization", "Location"));
    configuration.setAllowCredentials(!wildcard && !origins.isEmpty());

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
