package com.tata.identitysubscription.infrastructure.authorization.sfs;
import jakarta.servlet.http.HttpServletRequest;
final class PublicSessionEndpoints {
 private PublicSessionEndpoints() {}
 static boolean matches(HttpServletRequest request) {
  var p=request.getServletPath(); if(p.isEmpty()) p=request.getRequestURI().substring(request.getContextPath().length());
  if(p.startsWith("/v3/api-docs") || p.startsWith("/swagger-ui") || p.equals("/error") || request.getMethod().equals("OPTIONS")) return true;
  if(request.getMethod().equals("GET") && java.util.Set.of("/api/v1/plans","/health","/actuator/health").contains(p)) return true;
  return request.getMethod().equals("POST") && java.util.Set.of("/api/v1/accounts","/api/v1/accounts/verification","/api/v1/email-verification-requests","/api/v1/sessions","/api/v1/pin-sessions","/api/v1/care-links/acceptances").contains(p);
 }
}
