package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationDeliveryRequest;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationEmailProvider;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationPayloadCipher;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;
import com.vitahealth.tata.identitysubscription.infrastructure.email.queue.VerificationDeliveryDispatcher;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.VerificationDeliveryPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.VerificationDeliveryJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Exercises durable retry and cancellation with controlled time and no external email delivery. */
@SpringBootTest
@ActiveProfiles("test")
class VerificationDeliveryRecoveryIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-10-12T18:00:00Z");
  @Autowired AccountCommandService accounts;
  @Autowired VerificationDeliveryDispatcher dispatcher;
  @Autowired VerificationDeliveryJpaRepository deliveries;
  @Autowired VerificationPayloadCipher cipher;
  @MockitoBean VerificationEmailProvider provider;
  @MockitoBean Clock clock;

  @BeforeEach
  void resetTime() {
    when(clock.instant()).thenReturn(NOW);
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
  }

  @Test
  void retriesSamePayloadAfterUncertainProviderAcceptance() {
    var row = register();
    when(provider.send(any()))
        .thenThrow(new IllegalStateException("response lost"))
        .thenReturn("accepted-id");
    dispatcher.dispatch();
    assertNotNull(deliveries.findById(row.id()).orElseThrow().encryptedCode());
    dispatcher.dispatch();
    verify(provider).send(any());
    when(clock.instant()).thenReturn(NOW.plusSeconds(11));
    dispatcher.dispatch();
    var requests = ArgumentCaptor.forClass(VerificationDeliveryRequest.class);
    verify(provider, times(2)).send(requests.capture());
    assertEquals(requests.getAllValues().get(0), requests.getAllValues().get(1));
    assertNull(deliveries.findById(row.id()).orElseThrow().encryptedCode());
    deliveries.deleteById(row.id());
  }

  @Test
  void expirationCancelsPendingRetryWithoutSendingAgain() {
    var row = register();
    when(provider.send(any())).thenThrow(new IllegalStateException("provider unavailable"));
    dispatcher.dispatch();
    when(clock.instant()).thenReturn(NOW.plusSeconds(901));
    dispatcher.dispatch();
    verify(provider).send(any());
    assertNull(deliveries.findById(row.id()).orElseThrow().encryptedCode());
    deliveries.deleteById(row.id());
  }

  @Test
  void alreadyVerifiedAccountCancelsUnsentChallenge() {
    var row = register();
    String code = cipher.decrypt(row.id(), row.encryptedCode());
    assertTrue(accounts.verify(new VerifyEmailCommand(row.email(), code)).toOptional().isPresent());
    dispatcher.dispatch();
    verifyNoInteractions(provider);
    assertNull(deliveries.findById(row.id()).orElseThrow().encryptedCode());
    deliveries.deleteById(row.id());
  }

  private VerificationDeliveryPersistenceEntity register() {
    String email = UUID.randomUUID() + "@example.com";
    assertTrue(
        accounts
            .register(new RegisterFamilyAccountCommand("Rosa", email, "password-123"))
            .toOptional()
            .isPresent());
    return deliveries.findAll().stream()
        .filter(row -> row.email().equals(email))
        .findFirst()
        .orElseThrow();
  }
}
