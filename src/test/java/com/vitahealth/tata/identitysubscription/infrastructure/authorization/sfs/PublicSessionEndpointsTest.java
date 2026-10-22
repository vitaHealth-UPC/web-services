package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Verifies documentation prefix lookalikes do not become public API routes. */
class PublicSessionEndpointsTest {
  @Test
  void documentationRootsAndChildrenArePublicButSimilarPrefixesAreProtected() {
    for (String path :
        new String[] {
          "/v3/api-docs",
          "/v3/api-docs/swagger-config",
          "/swagger-ui/index.html",
          "/swagger-ui.html"
        }) {
      assertTrue(PublicSessionEndpoints.matches(new MockHttpServletRequest("GET", path)));
    }
    for (String path :
        new String[] {"/v3/api-docs-private", "/swagger-ui-private", "/swagger-ui.html/private"}) {
      assertFalse(PublicSessionEndpoints.matches(new MockHttpServletRequest("GET", path)));
    }
  }
}
