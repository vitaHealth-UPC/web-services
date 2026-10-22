package com.vitahealth.tata.identitysubscription.infrastructure.email.resend;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationEmailProvider;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Configures the Resend provider with external credentials and bounded HTTP waits. */
@Configuration
public class ResendEmailConfiguration {
  /** Creates the provider; missing credentials fail delivery rather than fabricate acceptance. */
  @Bean
  public VerificationEmailProvider verificationEmailProvider(
      @Value("${tata.email.resend.api-key:}") String apiKey,
      @Value("${tata.email.resend.sender:}") String sender,
      Clock clock) {
    var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    var factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(10));
    var client =
        RestClient.builder().baseUrl("https://api.resend.com").requestFactory(factory).build();
    return new ResendVerificationDeliveryAdapter(client, apiKey, sender, clock);
  }
}
