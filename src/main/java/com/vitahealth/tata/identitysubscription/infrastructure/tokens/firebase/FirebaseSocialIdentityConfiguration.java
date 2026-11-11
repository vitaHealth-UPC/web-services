package com.vitahealth.tata.identitysubscription.infrastructure.tokens.firebase;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.SocialIdentityVerifier;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FirebaseSocialIdentityConfiguration {
  @Bean
  SocialIdentityVerifier socialIdentityVerifier(@Value("${tata.identity.firebase.project-id:}") String projectId, Clock clock) {
    if (projectId.isBlank()) return token -> { throw new IllegalStateException("Social sign-in is not configured"); };
    var http = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    var factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(10));
    var source = new GoogleFirebaseCertificateSource(RestClient.builder().requestFactory(factory).build(), clock);
    return new FirebaseIdTokenVerifier(projectId, new FirebaseSigningKeyCache(source, clock), clock);
  }
}
