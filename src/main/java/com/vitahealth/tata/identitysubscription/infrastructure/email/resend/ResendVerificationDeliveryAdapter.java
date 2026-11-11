package com.vitahealth.tata.identitysubscription.infrastructure.email.resend;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationDeliveryRequest;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationEmailProvider;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.CredentialEmailPurpose;
import java.time.Clock;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Implements Resend's send-email protocol without logging recipient, code, or provider bodies. */
public final class ResendVerificationDeliveryAdapter implements VerificationEmailProvider {
  private final RestClient client;
  private final String apiKey;
  private final String sender;
  private final Clock clock;

  /** Uses an injected HTTP client with bounded timeouts and externally configured credentials. */
  public ResendVerificationDeliveryAdapter(
      RestClient client, String apiKey, String sender, Clock clock) {
    this.client = Objects.requireNonNull(client);
    this.apiKey = apiKey;
    this.sender = sender;
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public String send(VerificationDeliveryRequest request) {
    Objects.requireNonNull(request);
    if (!clock.instant().isBefore(request.expiresAt())) {
      throw new IllegalStateException("Verification delivery has expired");
    }
    if (apiKey == null || apiKey.isBlank() || sender == null || sender.isBlank()) {
      throw new IllegalStateException("Verification email provider is not configured");
    }
    try {
      var response =
          client
              .post()
              .uri("/emails")
              .headers(headers -> headers.setBearerAuth(apiKey))
              .header(
                  "Idempotency-Key",
                  (request.purpose() == CredentialEmailPurpose.PASSWORD_RECOVERY
                          ? "recovery-"
                          : "verification-")
                      + request.deliveryId())
              .contentType(MediaType.APPLICATION_JSON)
              .body(ResendEmailRequestAssembler.from(sender, request))
              .retrieve()
              .body(ResendEmailResponse.class);
      if (response == null || response.id() == null || response.id().isBlank()) {
        throw new IllegalStateException(
            "Verification email provider returned no acceptance identifier");
      }
      return response.id();
    } catch (RestClientException exception) {
      // Provider error bodies can contain personal data; do not retain them in the cause chain.
      throw new IllegalStateException("Verification email provider request failed");
    }
  }
}
