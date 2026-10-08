package com.tata.identitysubscription.infrastructure.authorization.sfs;
import com.tata.identitysubscription.application.models.AuthenticatedSession;
import com.tata.identitysubscription.application.queryservices.ResourceAuthorizationQueryService;
import java.lang.reflect.Type;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
@ControllerAdvice
public class ResourceBodyAuthorizationAdvice extends RequestBodyAdviceAdapter {
 private final ResourceAuthorizationQueryService authorization;
 public ResourceBodyAuthorizationAdvice(ResourceAuthorizationQueryService authorization) { this.authorization=authorization; }
 public boolean supports(MethodParameter p,Type type,Class<? extends HttpMessageConverter<?>> converter) { return true; }
 public Object afterBodyRead(Object body,HttpInputMessage input,MethodParameter p,Type type,Class<? extends HttpMessageConverter<?>> converter) {
  var attributes=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
  var req=input instanceof ServletServerHttpRequest servlet ? servlet.getServletRequest()
    : attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttributes ? servletAttributes.getRequest() : null;
  if(req==null) throw new IllegalStateException("Request context unavailable for body authorization");
  if(PublicSessionEndpoints.matches(req)) return body;
  var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null || !(auth.getPrincipal() instanceof AuthenticatedSession s)) return body;
  if(!body.getClass().isRecord()) return body;
  var selectors=Set.of("caregiverId","familiarId","accountId","userId","olderAdultId","medicationId","treatmentId","intakeId","careLinkId");
  for(var component:body.getClass().getRecordComponents()) {
   if(!selectors.contains(component.getName())) continue;
   try { var value=component.getAccessor().invoke(body); authorization.authorizeSelector(s,component.getName(),value==null?null:value.toString(),req.getRequestURI().substring(req.getContextPath().length()),req.getMethod()); }
   catch(ReflectiveOperationException ex) { throw new IllegalStateException("Cannot read resource selector",ex); }
  }
  return body;
 }
}
