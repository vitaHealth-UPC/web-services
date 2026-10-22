package com.vitahealth.tata.intakeexecution.infrastructure.speech.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.vitahealth.tata.intakeexecution.infrastructure.external.SpeechToTextProviderClient;
import com.vitahealth.tata.intakeexecution.infrastructure.speech.google.auth.ApplicationDefaultGoogleSpeechAuthorizationProvider;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Configures the real Google provider with bounded HTTP and lazily resolved external ADC. */
@Configuration
public class GoogleSpeechConfiguration {
  /** Owns and closes the reusable HTTP transport at application-context shutdown. */
  @Bean(destroyMethod = "close")
  public HttpClient googleSpeechHttpClient() {
    return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  /** Creates one real provider; credentials are only resolved for an explicitly enabled request. */
  @Bean
  public SpeechToTextProviderClient speechToTextProviderClient(
      @Value("${tata.speech.google.enabled:false}") boolean enabled,
      Clock clock,
      @Qualifier("googleSpeechHttpClient") HttpClient transport) {
    var factory = new JdkClientHttpRequestFactory(transport);
    factory.setReadTimeout(Duration.ofSeconds(30));
    var http =
        RestClient.builder()
            .baseUrl("https://speech.googleapis.com")
            .requestFactory(factory)
            .build();
    var credentials =
        new ApplicationDefaultGoogleSpeechAuthorizationProvider(
            GoogleSpeechConfiguration::loadCredentials, clock);
    return new GoogleSpeechToTextProviderClient(http, credentials, enabled);
  }

  private static GoogleCredentials loadCredentials() {
    try {
      return GoogleCredentials.getApplicationDefault()
          .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
    } catch (IOException unavailable) {
      throw new IllegalStateException("Google Speech credentials are unavailable");
    }
  }
}
