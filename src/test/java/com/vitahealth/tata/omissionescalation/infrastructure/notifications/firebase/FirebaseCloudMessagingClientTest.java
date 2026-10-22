package com.vitahealth.tata.omissionescalation.infrastructure.notifications.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

/**
 * Exercises real loopback HTTP serialization and receipt validation without Firebase credentials.
 */
class FirebaseCloudMessagingClientTest {
  private HttpServer server;
  private FirebaseCloudMessagingClient client;
  private int status = 200;
  private String response = "{\"name\":\"projects/tata-test/messages/accepted-1\"}";
  private final AtomicReference<String> body = new AtomicReference<>();
  private final AtomicReference<String> bearer = new AtomicReference<>();

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/v1/projects/tata-test/messages:send",
        exchange -> {
          bearer.set(exchange.getRequestHeaders().getFirst("Authorization"));
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          var bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    client =
        new FirebaseCloudMessagingClient(
            RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build(),
            () -> "test-credential",
            "tata-test");
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  @Test
  void sendsDeviceDataWithBearerAndRequiresProviderReceipt() {
    var receipt =
        client.send(new FirebaseMessage("device-secret", Map.of("kind", "CAREGIVER_ALERT")));
    assertThat(receipt.name()).isEqualTo("projects/tata-test/messages/accepted-1");
    assertThat(bearer.get()).isEqualTo("Bearer test-credential");
    assertThat(body.get())
        .contains("\"message\"", "\"token\":\"device-secret\"", "CAREGIVER_ALERT")
        .doesNotContain("topic", "validate_only");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"name\":\"projects/another/messages/1\"}",
        "{\"name\":\"projects/tata-test/messages/\"}",
        "not-json"
      })
  void rejectsMissingMalformedAndWrongProjectReceipts(String invalid) {
    response = invalid;
    assertRejected();
  }

  @Test
  void providerErrorDoesNotExposeResponseOrCredentials() {
    status = 403;
    response = "{\"error\":\"private-medication device-secret test-credential\"}";
    assertRejected();
  }

  private void assertRejected() {
    assertThatThrownBy(
            () ->
                client.send(
                    new FirebaseMessage("device-secret", Map.of("body", "private-medication"))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Firebase message was not accepted")
        .hasNoCause();
  }
}
