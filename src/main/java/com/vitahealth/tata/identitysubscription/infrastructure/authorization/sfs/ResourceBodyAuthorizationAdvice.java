package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import com.vitahealth.tata.identitysubscription.application.queryservices.ResourceAuthorizationQueryService;
import java.lang.reflect.Type;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/** Checks ownership selectors in private request records after HTTP deserialization. */
@ControllerAdvice
public class ResourceBodyAuthorizationAdvice extends RequestBodyAdviceAdapter {
  private final ResourceAuthorizationQueryService authorization;

  /** Uses the same application ownership policy as path and query authorization. */
  public ResourceBodyAuthorizationAdvice(ResourceAuthorizationQueryService authorization) {
    this.authorization = authorization;
  }

  /** Covers request bodies consistently; public routes are excluded after resolving the request. */
  @Override
  public boolean supports(
      MethodParameter p, Type type, Class<? extends HttpMessageConverter<?>> converter) {
    return true;
  }

  /** Requires a verified identity even when a private body contains no ownership selector. */
  @Override
  public Object afterBodyRead(
      Object body,
      HttpInputMessage input,
      MethodParameter p,
      Type type,
      Class<? extends HttpMessageConverter<?>> converter) {
    var attributes = RequestContextHolder.getRequestAttributes();
    var req =
        input instanceof ServletServerHttpRequest servlet
            ? servlet.getServletRequest()
            : attributes instanceof ServletRequestAttributes servletAttributes
                ? servletAttributes.getRequest()
                : null;
    if (req == null)
      throw new IllegalStateException("Request context unavailable for body authorization");
    if (PublicSessionEndpoints.matches(req)) return body;
    var s = AuthenticatedRequestIdentity.require();
    if (!body.getClass().isRecord()) return body;
    var selectors =
        Set.of(
            "caregiverId",
            "familiarId",
            "accountId",
            "userId",
            "olderAdultId",
            "medicationId",
            "treatmentId",
            "intakeId",
            "careLinkId");
    for (var component : body.getClass().getRecordComponents()) {
      if (!selectors.contains(component.getName())) continue;
      try {
        var value = component.getAccessor().invoke(body);
        authorization.authorizeSelector(
            s,
            component.getName(),
            value == null ? null : value.toString(),
            req.getRequestURI().substring(req.getContextPath().length()),
            req.getMethod());
      } catch (ReflectiveOperationException ex) {
        throw new IllegalStateException("Cannot read resource selector", ex);
      }
    }
    return body;
  }
}
