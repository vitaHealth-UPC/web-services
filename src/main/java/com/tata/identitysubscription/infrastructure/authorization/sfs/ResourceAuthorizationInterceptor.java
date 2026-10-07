package com.tata.identitysubscription.infrastructure.authorization.sfs;
import com.tata.identitysubscription.application.models.AuthenticatedSession;
import com.tata.identitysubscription.application.models.ResourceAccessDenied;
import com.tata.identitysubscription.application.queryservices.ResourceAuthorizationQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import java.util.Map;
final class ResourceAuthorizationInterceptor implements HandlerInterceptor {
 private final ResourceAuthorizationQueryService authorization;
 ResourceAuthorizationInterceptor(ResourceAuthorizationQueryService authorization) { this.authorization=authorization; }
 public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler) {
  if(PublicSessionEndpoints.matches(req)) return true;
  var authentication=SecurityContextHolder.getContext().getAuthentication();
  // Isolated MVC tests invoke application contracts without installing the security filter chain.
  // Real protected requests always pass SessionAuthenticationFilter first.
  if(authentication==null) return true;
  if(!(authentication.getPrincipal() instanceof AuthenticatedSession s)) throw new ResourceAccessDenied();
  var path=req.getRequestURI().substring(req.getContextPath().length());var method=req.getMethod();
  authorization.authorizeRequest(s,path,method);
  var variables=req.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
  if(variables instanceof Map<?,?> map) map.forEach((k,v) -> authorization.authorizeSelector(s,k.toString(),v.toString(),path,method));
  req.getParameterMap().forEach((k,values) -> { for(var value:values) authorization.authorizeSelector(s,k,value,path,method); });
  return true;
 }
}
