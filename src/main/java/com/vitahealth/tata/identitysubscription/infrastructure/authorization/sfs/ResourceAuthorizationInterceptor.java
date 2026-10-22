package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import com.vitahealth.tata.identitysubscription.application.queryservices.ResourceAuthorizationQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/** Enforces verified identity and ownership selectors before private controller execution. */
final class ResourceAuthorizationInterceptor implements HandlerInterceptor {
  private final ResourceAuthorizationQueryService authorization;

  ResourceAuthorizationInterceptor(ResourceAuthorizationQueryService authorization) {
    this.authorization = authorization;
  }

  /** Authorizes the route, path variables and query selectors of each protected request. */
  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
    if (PublicSessionEndpoints.matches(req)) return true;
    var s = AuthenticatedRequestIdentity.require();
    var path = req.getRequestURI().substring(req.getContextPath().length());
    var method = req.getMethod();
    authorization.authorizeRequest(s, path, method);
    var variables = req.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    if (variables instanceof Map<?, ?> map)
      map.forEach(
          (k, v) -> authorization.authorizeSelector(s, k.toString(), v.toString(), path, method));
    req.getParameterMap()
        .forEach(
            (k, values) -> {
              for (var value : values) authorization.authorizeSelector(s, k, value, path, method);
            });
    return true;
  }
}
