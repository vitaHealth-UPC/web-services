package com.vitahealth.tata.identitysubscription.infrastructure.email.resend;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationDeliveryRequest;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.CredentialEmailPurpose;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ResendPasswordRecoveryDeliveryTest {
  @Test
  void recoveryHasItsOwnSubjectTextAndIdempotencyPrefix() {
    var now = Instant.parse("2026-10-26T12:00:00Z");
    var builder = RestClient.builder().baseUrl("https://api.resend.com");
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://api.resend.com/emails"))
        .andExpect(header("Idempotency-Key", "recovery-recovery-1"))
        .andExpect(
            content()
                .json(
                    """
{"from":"Tata <sender@example.com>","to":["recipient@example.com"],"subject":"Recupera tu acceso a Tata","text":"Tu código para restablecer tu contraseña es 012345.\nVence el 2026-10-26T12:10:00Z.\nSi no solicitaste este cambio, puedes ignorar este mensaje."}
"""))
        .andRespond(withSuccess("{\"id\":\"accepted\"}", MediaType.APPLICATION_JSON));
    var adapter =
        new ResendVerificationDeliveryAdapter(
            builder.build(),
            "test-key",
            "Tata <sender@example.com>",
            Clock.fixed(now, ZoneOffset.UTC));
    assertEquals(
        "accepted",
        adapter.send(
            new VerificationDeliveryRequest(
                "recovery-1",
                "recipient@example.com",
                "012345",
                now.plusSeconds(600),
                CredentialEmailPurpose.PASSWORD_RECOVERY)));
    server.verify();
  }
}
