package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

/** Defines the shared public-route boundary used by authentication and ownership checks. */
final class PublicSessionEndpoints {
  private PublicSessionEndpoints() {}

  /** Allows only explicit public routes or documentation subpaths, with their supported methods. */
  static boolean matches(HttpServletRequest request) {
    var p = request.getServletPath();
    if (p.isEmpty()) p = request.getRequestURI().substring(request.getContextPath().length());
    return matches(request.getMethod(), p);
  }

  /** Shares the effective method/path policy with documentation without manufacturing requests. */
  static boolean matches(String method, String p) {
    if (isPathOrChild(p, "/v3/api-docs")
        || isPathOrChild(p, "/swagger-ui")
        || p.equals("/swagger-ui.html")
        || p.equals("/error")
        || method.equals("OPTIONS")) return true;
    if (method.equals("GET") && Set.of("/api/v1/plans", "/health", "/actuator/health").contains(p))
      return true;
    return method.equals("POST")
        && Set.of(
                "/api/v1/accounts",
                "/api/v1/accounts/verification",
                "/api/v1/email-verification-requests",
                "/api/v1/password-recovery-requests",
                "/api/v1/password-recoveries",
                "/api/v1/social-sessions",
                "/api/v1/sessions",
                "/api/v1/pin-sessions",
                "/api/v1/care-links/acceptances")
            .contains(p);
  }

  private static boolean isPathOrChild(String path, String root) {
    return path.equals(root) || path.startsWith(root + "/");
  }
}
