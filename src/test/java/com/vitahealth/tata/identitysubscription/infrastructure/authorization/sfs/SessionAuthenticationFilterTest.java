package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.queryservices.SessionQueryService;
import jakarta.servlet.ServletException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Verifies a rejected request cannot retain another request's authentication on the same thread.
 */
class SessionAuthenticationFilterTest {
  @Test
  void downstreamFailureStillClearsAuthenticatedIdentity() throws Exception {
    var sessions = mock(SessionQueryService.class);
    var identity =
        new AuthenticatedSession(
            "caregiver", AuthenticatedSession.Role.CAREGIVER, Instant.now().plusSeconds(60), null);
    when(sessions.authenticate("credential")).thenReturn(Optional.of(identity));
    var request = new MockHttpServletRequest("GET", "/api/v1/sessions/current");
    request.addHeader("Authorization", "Bearer credential");
    assertThrows(
        ServletException.class,
        () ->
            new SessionAuthenticationFilter(sessions)
                .doFilter(
                    request,
                    new MockHttpServletResponse(),
                    (incoming, response) -> {
                      assertEquals(
                          "caregiver",
                          SecurityContextHolder.getContext().getAuthentication().getName());
                      throw new ServletException("downstream failure");
                    }));
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void publicRequestDoesNotInheritPriorIdentityOrQueryCredentials() throws Exception {
    var sessions = mock(SessionQueryService.class);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("previous-user", null));
    var chain = new MockFilterChain();
    new SessionAuthenticationFilter(sessions)
        .doFilter(
            new MockHttpServletRequest("GET", "/api/v1/plans"),
            new MockHttpServletResponse(),
            chain);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(sessions);
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void absentBearerClearsPriorIdentityAndDoesNotReachProtectedHandler() throws Exception {
    var sessions = mock(SessionQueryService.class);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("previous-user", null));
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();
    new SessionAuthenticationFilter(sessions)
        .doFilter(new MockHttpServletRequest("GET", "/api/v1/sessions/current"), response, chain);
    assertEquals(401, response.getStatus());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertNull(chain.getRequest());
    verifyNoInteractions(sessions);
  }
}
