package com.tata.identitysubscription.infrastructure.authorization.sfs;
import com.tata.identitysubscription.application.queryservices.SessionQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
final class SessionAuthenticationFilter extends OncePerRequestFilter {
 private final SessionQueryService sessions;
 SessionAuthenticationFilter(SessionQueryService sessions) { this.sessions=sessions; }
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
  if(PublicSessionEndpoints.matches(req)) { chain.doFilter(req,res); return; }
  var header=req.getHeader("Authorization");
  var session=header!=null && header.startsWith("Bearer ") ? sessions.authenticate(header.substring(7)) : java.util.Optional.<com.tata.identitysubscription.application.models.AuthenticatedSession>empty();
  if(session.isEmpty()) { res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"A valid session is required\"}");return; }
  var principal=session.get();var context=SecurityContextHolder.createEmptyContext();
  context.setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,List.of(new SimpleGrantedAuthority("ROLE_"+principal.role().name()))));
  SecurityContextHolder.setContext(context);
  try { chain.doFilter(req,res); } finally { SecurityContextHolder.clearContext(); }
 }
}
