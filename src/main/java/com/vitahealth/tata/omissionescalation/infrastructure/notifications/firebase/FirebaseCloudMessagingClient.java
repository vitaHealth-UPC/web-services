package com.vitahealth.tata.omissionescalation.infrastructure.notifications.firebase;

import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Sends authorized device data messages through the real FCM HTTP v1 protocol. */
public final class FirebaseCloudMessagingClient {
  private final RestClient client;
  private final Supplier<String> accessToken;
  private final String projectId;

  /**
   * Injects timeout-bounded HTTP and renewable OAuth credentials without loading them at startup.
   */
  public FirebaseCloudMessagingClient(
      RestClient client, Supplier<String> accessToken, String projectId) {
    this.client = Objects.requireNonNull(client, "client");
    this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
    if (projectId == null || !projectId.matches("[a-z][a-z0-9-]{4,28}[a-z0-9]"))
      throw new IllegalArgumentException("Firebase project id is invalid");
    this.projectId = projectId;
  }

  /** Returns a validated provider receipt, without claiming that a device displayed the message. */
  public FirebaseSendReceipt send(FirebaseMessage message) {
    Objects.requireNonNull(message, "message");
    try {
      var credential = accessToken.get();
      if (credential == null || credential.isBlank()) throw unavailable();
      var receipt =
          client
              .post()
              .uri("/v1/projects/{project}/messages:send", projectId)
              .contentType(MediaType.APPLICATION_JSON)
              .headers(headers -> headers.setBearerAuth(credential))
              .body(new FirebaseSendRequest(message))
              .retrieve()
              .body(FirebaseSendReceipt.class);
      var prefix = "projects/" + projectId + "/messages/";
      if (receipt == null
          || receipt.name() == null
          || !receipt.name().startsWith(prefix)
          || receipt.name().substring(prefix.length()).isBlank()) throw unavailable();
      return receipt;
    } catch (RuntimeException failure) {
      // Provider exceptions can contain the request, token or clinical response; retain no cause.
      throw unavailable();
    }
  }

  private static IllegalStateException unavailable() {
    return new IllegalStateException("Firebase message was not accepted");
  }
}
