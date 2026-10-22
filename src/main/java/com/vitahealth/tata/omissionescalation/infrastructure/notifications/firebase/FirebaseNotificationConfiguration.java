package com.vitahealth.tata.omissionescalation.infrastructure.notifications.firebase;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationDeviceTransport;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Configures actual FCM HTTP v1 with external credentials and bounded network waits. */
@Configuration
public class FirebaseNotificationConfiguration {
  /** Reuses and closes the FCM transport with the application context lifecycle. */
  @Bean(destroyMethod = "close")
  public HttpClient firebaseNotificationHttpClient() {
    return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  /** Keeps disabled deployments unavailable rather than pretending that log output is delivery. */
  @Bean
  public NotificationDeviceTransport notificationDeviceTransport(
      @Value("${tata.notifications.firebase.enabled:false}") boolean enabled,
      @Value("${tata.notifications.firebase.project-id:}") String projectId,
      @Qualifier("firebaseNotificationHttpClient") HttpClient transport,
      Clock clock) {
    if (!enabled)
      return request -> {
        throw new IllegalStateException("Firebase notification delivery is disabled");
      };
    var factory = new JdkClientHttpRequestFactory(transport);
    factory.setReadTimeout(Duration.ofSeconds(15));
    var client =
        new FirebaseCloudMessagingClient(
            RestClient.builder()
                .baseUrl("https://fcm.googleapis.com")
                .requestFactory(factory)
                .build(),
            new FirebaseAccessTokenProvider(clock),
            projectId);
    return request -> client.send(FirebaseNotificationMessageAssembler.toMessage(request)).name();
  }
}
