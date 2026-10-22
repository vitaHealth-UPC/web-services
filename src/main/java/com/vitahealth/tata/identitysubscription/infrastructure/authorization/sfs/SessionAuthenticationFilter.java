package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.queryservices.SessionQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates protected requests and clears their security context after completion. */
final class SessionAuthenticationFilter extends OncePerRequestFilter {
  private final SessionQueryService sessions;

  /** Injects credential lookup independently of token format and persistence implementation. */
  SessionAuthenticationFilter(SessionQueryService sessions) {
    this.sessions = sessions;
  }

  /**
   * Requires fresh bearer identity for private requests and never reuses a thread's prior context.
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    SecurityContextHolder.clearContext();
    if (PublicSessionEndpoints.matches(req)) {
      chain.doFilter(req, res);
      return;
    }
    var header = req.getHeader("Authorization");
    var session =
        header != null && header.startsWith("Bearer ")
            ? sessions.authenticate(header.substring(7))
            : Optional.<AuthenticatedSession>empty();
    if (session.isEmpty()) {
      res.setStatus(HttpStatus.UNAUTHORIZED.value());
      res.setContentType(MediaType.APPLICATION_JSON_VALUE);
      res.getWriter()
          .write(
              "{\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"A valid session is required\"}");
      return;
    }
    var identity = session.get();
    var principal = new SessionPrincipal(identity);
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + identity.role().name()))));
    SecurityContextHolder.setContext(context);
    try {
      chain.doFilter(req, res);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
