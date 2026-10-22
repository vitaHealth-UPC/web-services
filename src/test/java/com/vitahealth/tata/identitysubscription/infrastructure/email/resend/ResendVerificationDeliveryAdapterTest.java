package com.vitahealth.tata.identitysubscription.infrastructure.email.resend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationDeliveryRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Exercises the actual HTTP protocol without sending email to external recipients. */
class ResendVerificationDeliveryAdapterTest {
  private final Instant now = Instant.parse("2026-10-12T12:00:00Z");
  private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
  private final VerificationDeliveryRequest request =
      new VerificationDeliveryRequest(
          "challenge-1", "recipient@example.com", "012345", now.plusSeconds(900));

  @Test
  void sendsStablePayloadAndIdempotencyKey() {
    var builder = RestClient.builder().baseUrl("https://api.resend.com");
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://api.resend.com/emails"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-key"))
        .andExpect(header("Idempotency-Key", "verification-challenge-1"))
        .andExpect(
            content()
                .json(
                    """
{"from":"Tata <sender@example.com>","to":["recipient@example.com"],
"subject":"Verifica tu cuenta de Tata",
"text":"Tu código de verificación es 012345.\\nVence el 2026-10-12T12:15:00Z.\\nSi no solicitaste esta cuenta, puedes ignorar este mensaje."}
"""))
        .andRespond(withSuccess("{\"id\":\"accepted-1\"}", MediaType.APPLICATION_JSON));
    var adapter =
        new ResendVerificationDeliveryAdapter(
            builder.build(), "test-key", "Tata <sender@example.com>", clock);
    assertEquals("accepted-1", adapter.send(request));
    server.verify();
  }

  @Test
  void rejectsMissingAcceptanceAndProviderErrors() {
    var builder = RestClient.builder().baseUrl("https://api.resend.com");
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://api.resend.com/emails"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    server.expect(requestTo("https://api.resend.com/emails")).andRespond(withServerError());
    var adapter =
        new ResendVerificationDeliveryAdapter(
            builder.build(), "test-key", "sender@example.com", clock);
    assertThrows(IllegalStateException.class, () -> adapter.send(request));
    assertThrows(IllegalStateException.class, () -> adapter.send(request));
    server.verify();
  }

  @Test
  void rejectsExpiredAndUnconfiguredRequestsBeforeHttp() {
    var builder = RestClient.builder().baseUrl("https://api.resend.com");
    var server = MockRestServiceServer.bindTo(builder).build();
    var adapter = new ResendVerificationDeliveryAdapter(builder.build(), "", "", clock);
    assertThrows(IllegalStateException.class, () -> adapter.send(request));
    var configured =
        new ResendVerificationDeliveryAdapter(builder.build(), "key", "sender@example.com", clock);
    var expired =
        new VerificationDeliveryRequest("challenge-2", request.email(), request.code(), now);
    assertThrows(IllegalStateException.class, () -> configured.send(expired));
    server.verify();
    assertFalse(request.toString().contains(request.email()));
    assertFalse(request.toString().contains(request.code()));
  }
}
