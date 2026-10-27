package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationPayloadCipher;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.*;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class PasswordRecoveryApiIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired AccountRepository accounts;
  @Autowired PasswordHasher hasher;
  @Autowired VerificationPayloadCipher cipher;
  @Autowired SessionTokenService tokens;
  @Autowired VerificationDeliveryJpaRepository deliveries;
  @Autowired PasswordRecoveryJpaRepository recoveries;
  @Autowired AccountSessionJpaRepository sessions;
  @Autowired AccountJpaRepository accountRows;
  @Autowired PlatformTransactionManager manager;
  @Autowired Clock clock;
  MockMvc mvc;
  final List<String> created = new ArrayList<>();

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  Account account(boolean active) {
    var value =
        Account.register(
            "Caregiver",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            hasher.hash("old-password"),
            "verification",
            clock.instant(),
            Duration.ofMinutes(15));
    if (active) value.completeVerification();
    value = accounts.save(value);
    created.add(value.id());
    return value;
  }

  @AfterEach
  void cleanup() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            s -> {
              deliveries.findAll().stream()
                  .filter(row -> created.contains(row.accountId()))
                  .forEach(deliveries::delete);
              for (var id : created) {
                sessions.revokeCaregiverSessions(id);
                recoveries.findById(id).ifPresent(recoveries::delete);
                accountRows.deleteById(id);
              }
            });
  }

  void request(String email) throws Exception {
    mvc.perform(
            post("/api/v1/password-recovery-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isAccepted())
        .andExpect(content().string(""));
  }

  String completion(String email, String code, String password) {
    return "{\"email\":\""
        + email
        + "\",\"code\":\""
        + code
        + "\",\"newPassword\":\""
        + password
        + "\"}";
  }

  @Test
  void anonymousRequestIsUniformForActivePendingAndMissingEmailAndThrottlesResend()
      throws Exception {
    var active = account(true);
    var pending = account(false);
    request(active.email().value());
    request(pending.email().value());
    request(UUID.randomUUID() + "@example.com");
    var first = recoveries.findById(active.id()).orElseThrow();
    request(active.email().value());
    assertEquals(first.getCodeHash(), recoveries.findById(active.id()).orElseThrow().getCodeHash());
    assertEquals(
        1,
        deliveries.findAll().stream().filter(row -> row.accountId().equals(active.id())).count());
    assertTrue(recoveries.findById(pending.id()).isEmpty());
  }

  @Test
  void publicCompletionChangesActualLoginAndRevokesTheOldSession() throws Exception {
    var account = account(true);
    var oldToken = tokens.issue(account.id()).accessToken();
    request(account.email().value());
    var delivery =
        deliveries.findAll().stream()
            .filter(row -> row.accountId().equals(account.id()))
            .findFirst()
            .orElseThrow();
    var code = cipher.decrypt(delivery.id(), delivery.encryptedCode());
    mvc.perform(
            post("/api/v1/password-recoveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(completion(account.email().value(), code, "new-password")))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    mvc.perform(get("/api/v1/sessions/current").header("Authorization", "Bearer " + oldToken))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\""
                        + account.email().value()
                        + "\",\"password\":\"new-password\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").value(account.id()))
        .andExpect(jsonPath("$.accessToken").exists());
    mvc.perform(
            post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\""
                        + account.email().value()
                        + "\",\"password\":\"old-password\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/password-recoveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(completion(account.email().value(), code, "another-password")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("INVALID_RECOVERY_CODE"));
  }

  @Test
  void invalidRecoveryEvidenceHasTheSamePublicLocalizedError() throws Exception {
    var account = account(true);
    for (var email : List.of(account.email().value(), UUID.randomUUID() + "@example.com")) {
      mvc.perform(
              post("/api/v1/password-recoveries")
                  .header("Accept-Language", "es")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(completion(email, "000000", "new-password")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("INVALID_RECOVERY_CODE"))
          .andExpect(
              jsonPath("$.detail")
                  .value("El código de recuperación no es válido o ya no está disponible."));
    }
  }
}
