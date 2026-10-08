package com.tata.identitysubscription.infrastructure.authorization.sfs;
import com.tata.identitysubscription.application.queryservices.ResourceAuthorizationQueryService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class AuthorizationMvcConfiguration implements WebMvcConfigurer {
 private final ResourceAuthorizationQueryService authorization;
 public AuthorizationMvcConfiguration(ResourceAuthorizationQueryService authorization) { this.authorization=authorization; }
 public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(new ResourceAuthorizationInterceptor(authorization)); }
}
