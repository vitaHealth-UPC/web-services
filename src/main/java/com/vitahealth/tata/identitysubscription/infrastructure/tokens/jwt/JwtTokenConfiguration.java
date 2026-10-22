package com.vitahealth.tata.identitysubscription.infrastructure.tokens.jwt;

import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.opaque.OpaqueSessionTokenService;
import com.vitahealth.tata.identitysubscription.infrastructure.tokens.persistence.PersistedSessionRegistry;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Configures JWT issuance with an external key and explicit persisted legacy compatibility. */
@Configuration
public class JwtTokenConfiguration {
  /** Requires a configured signing key; no production default or ephemeral key is generated. */
  @Bean
  public JwtTokenCodec jwtTokenCodec(
      @Value("${tata.security.jwt.secret-base64:}") String key, Clock clock) {
    return new JwtTokenCodec(key, clock);
  }

  /** Selects signed sessions for application ports while preserving existing opaque sessions. */
  @Bean
  @Primary
  public JwtSessionTokenService jwtSessionTokenService(
      JwtTokenCodec codec,
      OpaqueSessionTokenService legacy,
      CareLinkContextFacade links,
      Clock clock,
      PersistedSessionRegistry registry) {
    return new JwtSessionTokenService(codec, legacy, links, clock, registry);
  }
}
