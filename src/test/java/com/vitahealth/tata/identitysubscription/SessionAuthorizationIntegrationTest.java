package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.identitysubscription.application.commandservices.PinCommandService;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.commands.AuthenticateWithPinCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPinCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.domain.repositories.PinCredentialRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import com.vitahealth.tata.shared.application.result.Result;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class SessionAuthorizationIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired AccountRepository accounts;
  @Autowired PasswordHasher hasher;
  @Autowired SessionTokenService tokens;
  @Autowired AccountSessionJpaRepository sessions;
  @Autowired CareLinkCommandService links;
  @Autowired PinCommandService pins;
  @Autowired PinCredentialRepository credentials;
  MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  Account account(boolean active) {
    var a =
        Account.register(
            "Caregiver",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            hasher.hash("password123"),
            hasher.hash("123456"),
            Instant.now(),
            Duration.ofMinutes(15));
    if (active) a.completeVerification();
    return accounts.save(a);
  }

  String bearer(Account a) {
    return "Bearer " + tokens.issue(a.id()).accessToken();
  }

  @Test
  void adherenceViewsRequireTheAdultOrACurrentlyConsentedCaregiver() throws Exception {
    var caregiver = account(true);
    var stranger = account(true);
    var adult =
        links
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    caregiver.id(), "Rosa", LocalDate.of(1950, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    var generated =
        links
            .generateLinkingCode(new GenerateLinkingCodeCommand(caregiver.id(), adult.id()))
            .toOptional()
            .orElseThrow();
    var link =
        links
            .accept(
                new com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand(
                    caregiver.id(), generated.linkingCode()))
            .toOptional()
            .orElseThrow();
    links
        .registerConsent(
            new com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand(
                link.id(), true))
        .toOptional()
        .orElseThrow();
    var caregiverHeader = bearer(caregiver);
    var strangerHeader = bearer(stranger);
    var adultHeader = "Bearer " + tokens.issueOlderAdult(adult.id(), link.id()).accessToken();
    for (var suffix : new String[] {"summary", "insight", "insights"}) {
      var path = "/api/v1/older-adults/" + adult.id() + "/adherence/" + suffix;
      mvc.perform(get(path)).andExpect(status().isUnauthorized());
      mvc.perform(get(path).header("Authorization", strangerHeader))
          .andExpect(status().isForbidden());
      mvc.perform(get(path).header("Authorization", caregiverHeader))
          .andExpect(status().isNoContent());
      mvc.perform(get(path).header("Authorization", adultHeader)).andExpect(status().isNoContent());
      mvc.perform(
              get(path.replace(adult.id(), UUID.randomUUID().toString()))
                  .header("Authorization", adultHeader))
          .andExpect(status().isForbidden());
    }
    links
        .withdrawConsent(
            new com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand(
                link.id(), adult.id()))
        .toOptional()
        .orElseThrow();
    for (var suffix : new String[] {"summary", "insight", "insights"}) {
      mvc.perform(
              get("/api/v1/older-adults/" + adult.id() + "/adherence/" + suffix)
                  .header("Authorization", caregiverHeader))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void notificationPolicyCanOnlyBeChangedAndReadByItsOwner() throws Exception {
    var owner = account(true);
    var other = account(true);
    var header = bearer(owner);
    var path = "/api/v1/users/" + owner.id() + "/notification-preferences/policy";
    var body =
        """
        {"policy":{"reminderMinutes":10,"pendingAlertMinutes":20,"omittedAlertMinutes":120,
        "escalationMinutes":180,"dailySummaryTime":"20:00"},
        "contacts":[{"name":"Rosa","phone":"+51999123456"}]}
        """;
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
                .header("Authorization", bearer(other))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
                .header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emergencyContacts[0].name").value("Rosa"));
    mvc.perform(
            get("/api/v1/users/" + owner.id() + "/preferences")
                .header("Authorization", bearer(other)))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousInvalidAndExpiredSessionsCannotReadProtectedResources() throws Exception {
    mvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/api/v1/care-links").param("caregiverId", UUID.randomUUID().toString()))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/sessions/current").header("Authorization", "Bearer invalid"))
        .andExpect(status().isUnauthorized());
    var a = account(true);
    var token = "expired-" + UUID.randomUUID();
    var hash =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
    sessions.save(new AccountSessionPersistenceEntity(a.id(), hash, Instant.now().minusSeconds(1)));
    mvc.perform(get("/api/v1/sessions/current").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void caregiverCannotSpoofQueryPathOrBodyOwnership() throws Exception {
    var a = account(true);
    var b = account(true);
    var header = bearer(a);
    mvc.perform(
            get("/api/v1/care-links").param("caregiverId", a.id()).header("Authorization", header))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/care-links").param("caregiverId", b.id()).header("Authorization", header))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/accounts/" + b.id() + "/subscription").header("Authorization", header))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/older-adults")
                .header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"caregiverId\":\""
                        + b.id()
                        + "\",\"fullName\":\"Ana\",\"birthDate\":\"1950-01-01\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void linkAcceptanceOnlyAllowsOwnConsentUntilConfirmedAndThenOwnAdultData() throws Exception {
    var a = account(true);
    var adult =
        links
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    a.id(), "Ana", LocalDate.of(1950, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    var link =
        links
            .generateLinkingCode(new GenerateLinkingCodeCommand(a.id(), adult.id()))
            .toOptional()
            .orElseThrow();
    var acceptance =
        mvc.perform(
                post("/api/v1/care-links/acceptances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"caregiverId\":\""
                            + a.id()
                            + "\",\"code\":\""
                            + link.linkingCode()
                            + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String setup = com.jayway.jsonpath.JsonPath.read(acceptance, "$.accessToken");
    mvc.perform(
            get("/api/v1/older-adults/" + adult.id() + "/intakes/next")
                .header("Authorization", "Bearer " + setup))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/care-links/" + link.id() + "/consent")
                .header("Authorization", bearer(a))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepted\":true}"))
        .andExpect(status().isForbidden());
    var consent =
        mvc.perform(
                post("/api/v1/care-links/" + link.id() + "/consent")
                    .header("Authorization", "Bearer " + setup)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accepted\":true}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String adultToken = com.jayway.jsonpath.JsonPath.read(consent, "$.accessToken");
    mvc.perform(
            get("/api/v1/older-adults/" + adult.id() + "/intakes/next")
                .header("Authorization", "Bearer " + adultToken))
        .andExpect(status().isNoContent());
    mvc.perform(
            get("/api/v1/older-adults/" + UUID.randomUUID() + "/intakes/next")
                .header("Authorization", "Bearer " + adultToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void verificationEstablishesCaregiverSessionAndLogoutRevokesIt() throws Exception {
    var a = account(false);
    var result =
        mvc.perform(
                post("/api/v1/accounts/verification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + a.email().value() + "\",\"code\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(a.id()))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = com.jayway.jsonpath.JsonPath.read(result, "$.accessToken");
    var header = "Bearer " + token;
    mvc.perform(get("/api/v1/sessions/current").header("Authorization", header))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("CAREGIVER"))
        .andExpect(jsonPath("$.tokenHash").doesNotExist());
    mvc.perform(delete("/api/v1/sessions").header("Authorization", header))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/sessions/current").header("Authorization", header))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void failedPinAttemptsCommitAndLockEvenWhenAuthenticationThrows() {
    var owner = UUID.randomUUID().toString();
    pins.register(new RegisterPinCommand(owner, "1234"));
    for (int i = 1; i <= 4; i++) {
      var result = pins.authenticate(new AuthenticateWithPinCommand(owner, "9999"));
      assertInstanceOf(Result.Failure.class, result);
      assertEquals(i, credentials.findByOlderAdultId(owner).orElseThrow().failedAttempts());
    }
    var locked = credentials.findByOlderAdultId(owner).orElseThrow();
    assertNotNull(locked.lockedUntil());
    assertInstanceOf(
        Result.Failure.class, pins.authenticate(new AuthenticateWithPinCommand(owner, "1234")));
  }

  @Test
  void concurrentPinFailuresDoNotLoseAttempts() throws Exception {
    var owner = UUID.randomUUID().toString();
    pins.register(new RegisterPinCommand(owner, "1234"));
    var start = new java.util.concurrent.CountDownLatch(1);
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
      for (int i = 0; i < 4; i++)
        futures.add(
            executor.submit(
                () -> {
                  try {
                    start.await();
                    assertInstanceOf(
                        Result.Failure.class,
                        pins.authenticate(new AuthenticateWithPinCommand(owner, "9999")));
                  } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                  }
                }));
      start.countDown();
      for (var future : futures) future.get(30, java.util.concurrent.TimeUnit.SECONDS);
    }
    var credential = credentials.findByOlderAdultId(owner).orElseThrow();
    assertEquals(4, credential.failedAttempts());
    assertNotNull(credential.lockedUntil());
  }
}
