package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.exceptions.ResourceAccessDenied;
import com.vitahealth.tata.identitysubscription.application.queryservices.ResourceAuthorizationQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

/** Exercises authorization boundaries independently of the security filter chain. */
class ResourceAuthorizationBoundaryTest {
  private final ResourceAuthorizationQueryService authorization =
      mock(ResourceAuthorizationQueryService.class);
  private final ResourceAuthorizationInterceptor interceptor =
      new ResourceAuthorizationInterceptor(authorization);
  private final ResourceBodyAuthorizationAdvice advice =
      new ResourceBodyAuthorizationAdvice(authorization);

  @AfterEach
  void clearIdentity() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void privateRouteAndBodyRejectAbsentIdentity() {
    assertPrivateRejected();
  }

  @Test
  void privateRouteAndBodyRejectAnonymousOrForeignIdentity() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    assertPrivateRejected();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "foreign", null, List.of(new SimpleGrantedAuthority("ROLE_CAREGIVER"))));
    assertPrivateRejected();
  }

  @Test
  void privateRouteRejectsUnauthenticatedSessionPrincipal() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(new SessionPrincipal(identity()), null));
    assertPrivateRejected();
  }

  @Test
  void verifiedIdentityReachesRouteAndSelectorPolicies() {
    var session = identity();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new SessionPrincipal(session),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CAREGIVER"))));
    var request = new MockHttpServletRequest("GET", "/api/v1/accounts/account-1/subscription");
    request.setAttribute(
        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("accountId", "account-1"));
    assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    verify(authorization).authorizeRequest(session, request.getRequestURI(), "GET");
    verify(authorization)
        .authorizeSelector(session, "accountId", "account-1", request.getRequestURI(), "GET");
  }

  @Test
  void publicRegistrationDoesNotRequireIdentity() {
    var request = new MockHttpServletRequest("POST", "/api/v1/accounts");
    assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    Object body = new Object();
    assertSame(
        body,
        advice.afterBodyRead(
            body, new ServletServerHttpRequest(request), null, Object.class, null));
    verifyNoInteractions(authorization);
  }

  private void assertPrivateRejected() {
    var request = new MockHttpServletRequest("POST", "/api/v1/medications");
    assertThrows(
        ResourceAccessDenied.class,
        () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    assertThrows(
        ResourceAccessDenied.class,
        () ->
            advice.afterBodyRead(
                new Object(), new ServletServerHttpRequest(request), null, Object.class, null));
    verifyNoInteractions(authorization);
  }

  private AuthenticatedSession identity() {
    return new AuthenticatedSession(
        "account-1", AuthenticatedSession.Role.CAREGIVER, Instant.now().plusSeconds(60), null);
  }
}
