package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import com.vitahealth.tata.identitysubscription.application.queryservices.ResourceAuthorizationQueryService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Installs application ownership checks after the request's security identity is resolved. */
@Configuration
public class AuthorizationMvcConfiguration implements WebMvcConfigurer {
  private final ResourceAuthorizationQueryService authorization;

  /** Injects the policy used for route, path and query ownership selectors. */
  public AuthorizationMvcConfiguration(ResourceAuthorizationQueryService authorization) {
    this.authorization = authorization;
  }

  /**
   * Applies the ownership interceptor to MVC requests; public exclusions use the shared matcher.
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new ResourceAuthorizationInterceptor(authorization));
  }
}
