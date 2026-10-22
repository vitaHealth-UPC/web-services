package com.vitahealth.tata.shared.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

/** Verifies effective origin and credential policy after configuration normalization. */
class WebCorsConfigurationTest {
  private CorsConfiguration configuration(String origins) {
    return new WebCorsConfiguration()
        .corsConfigurationSource(origins)
        .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/session"));
  }

  @Test
  void wildcardWithTrailingCommaDoesNotPermitCredentials() {
    var policy = configuration(" *, ");
    assertFalse(policy.getAllowCredentials());
    assertEquals("https://client.example", policy.checkOrigin("https://client.example"));
    policy.validateAllowCredentials();
  }

  @Test
  void wildcardMixedWithExplicitOriginsRemainsUncredentialed() {
    var policy = configuration("https://client.example, *");
    assertFalse(policy.getAllowCredentials());
    policy.validateAllowCredentials();
  }

  @Test
  void explicitOriginsPermitCredentialsOnlyForListedClients() {
    var policy = configuration(" https://client.example, ");
    assertTrue(policy.getAllowCredentials());
    assertEquals("https://client.example", policy.checkOrigin("https://client.example"));
    assertEquals(null, policy.checkOrigin("https://other.example"));
  }

  @Test
  void emptyOriginListDoesNotPermitCredentials() {
    assertFalse(configuration(" , ").getAllowCredentials());
  }
}
