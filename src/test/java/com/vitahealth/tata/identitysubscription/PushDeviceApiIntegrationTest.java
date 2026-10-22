package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.identitysubscription.application.commandservices.PushDeviceCommandService;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPushDeviceCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.domain.repositories.PushDeviceRegistrationRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.PushDeviceRegistrationJpaRepository;
import com.vitahealth.tata.identitysubscription.interfaces.acl.PushDeviceContextFacade;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies actual authenticated device writes, rollback conflicts and session ownership over HTTP.
 */
@SpringBootTest
@ActiveProfiles("test")
class PushDeviceApiIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired AccountRepository accounts;
  @Autowired SessionTokenService tokens;
  @Autowired PushDeviceRegistrationRepository devices;
  @Autowired PushDeviceRegistrationJpaRepository rows;
  @Autowired Clock clock;
  @Autowired PushDeviceContextFacade recipientDevices;
  @Autowired PushDeviceCommandService commands;
  @Autowired PlatformTransactionManager transactions;
  @Autowired CareLinkCommandService links;
  private MockMvc mvc;
  private final List<String> installations = new ArrayList<>();
  private final List<String> credentials = new ArrayList<>();

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterEach
  void cleanup() {
    rows.deleteAllById(installations);
    credentials.forEach(tokens::revoke);
  }

  private String credential() {
    var account =
        Account.register(
            "Device owner",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            "hash",
            "challenge",
            clock.instant(),
            Duration.ofMinutes(15));
    account.completeVerification();
    var raw = tokens.issue(accounts.save(account).id()).accessToken();
    credentials.add(raw);
    return raw;
  }

  private String installation() {
    var id = UUID.randomUUID().toString();
    installations.add(id);
    return id;
  }

  private String path(String id) {
    return "/api/v1/push-devices/" + id;
  }

  private String payload(String token) {
    return "{\"token\":\"" + token + "\"}";
  }

  @Test
  void unauthenticatedAndInvalidRequestsCannotRegisterDevices() throws Exception {
    var id = installation();
    mvc.perform(put(path(id)).contentType(MediaType.APPLICATION_JSON).content(payload("token")))
        .andExpect(status().isUnauthorized());
    var raw = credential();
    mvc.perform(
            put(path(id))
                .header("Authorization", "Bearer " + raw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(" ")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put(path("bad-id"))
                .header("Authorization", "Bearer " + raw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("token")))
        .andExpect(status().isBadRequest());
    assertThat(devices.findByInstallationId(id)).isEmpty();
  }

  @Test
  void registerRetryAndRotationPreserveSessionAndHideTokenInResponse() throws Exception {
    var id = installation();
    var raw = credential();
    var token = UUID.randomUUID().toString();
    mvc.perform(
            put(path(id))
                .header("Authorization", "Bearer " + raw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.installationId").value(id))
        .andExpect(jsonPath("$.token").doesNotExist())
        .andExpect(jsonPath("$.sessionId").doesNotExist());
    var initial = devices.findByInstallationId(id).orElseThrow();
    mvc.perform(
            put(path(id))
                .header("Authorization", "Bearer " + raw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(token)))
        .andExpect(status().isOk());
    assertThat(devices.findByInstallationId(id)).contains(initial);
    mvc.perform(
            put(path(id))
                .header("Authorization", "Bearer " + raw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("rotated:" + token)))
        .andExpect(status().isOk());
    var rotated = devices.findByInstallationId(id).orElseThrow();
    assertThat(rotated.sessionId()).isEqualTo(initial.sessionId());
    assertThat(rotated.createdAt()).isEqualTo(initial.createdAt());
    assertThat(rotated.token()).isEqualTo("rotated:" + token);
  }

  @Test
  void anotherLiveSessionCannotRotateOrDeleteRegistration() throws Exception {
    var id = installation();
    var owner = credential();
    var other = credential();
    mvc.perform(
            put(path(id))
                .header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID().toString())))
        .andExpect(status().isOk());
    var before = devices.findByInstallationId(id).orElseThrow();
    mvc.perform(
            put(path(id))
                .header("Authorization", "Bearer " + other)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("other-token")))
        .andExpect(status().isConflict());
    mvc.perform(delete(path(id)).header("Authorization", "Bearer " + other))
        .andExpect(status().isForbidden());
    assertThat(devices.findByInstallationId(id)).contains(before);
  }

  @Test
  void duplicateTokenIsConflictAfterRollbackAndOriginalRegistrationSurvives() throws Exception {
    var first = installation();
    var second = installation();
    var raw = credential();
    var token = UUID.randomUUID().toString();
    mvc.perform(
            put(path(first))
                .header("Authorization", "Bearer " + raw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(token)))
        .andExpect(status().isOk());
    mvc.perform(
            put(path(second))
                .header("Authorization", "Bearer " + raw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(token)))
        .andExpect(status().isConflict());
    assertThat(devices.findByInstallationId(first)).isPresent();
    assertThat(devices.findByInstallationId(second)).isEmpty();
    mvc.perform(
            put(path(second))
                .header("Authorization", "Bearer " + raw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID().toString())))
        .andExpect(status().isOk());
  }

  @Test
  void revokedOwnerCannotWriteAndNewSessionCanReplaceInactiveRegistration() throws Exception {
    var id = installation();
    var old = credential();
    var replacement = credential();
    mvc.perform(
            put(path(id))
                .header("Authorization", "Bearer " + old)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID().toString())))
        .andExpect(status().isOk());
    var previousSession = devices.findByInstallationId(id).orElseThrow().sessionId();
    tokens.revoke(old);
    mvc.perform(delete(path(id)).header("Authorization", "Bearer " + old))
        .andExpect(status().isUnauthorized());
    var newToken = UUID.randomUUID().toString();
    mvc.perform(
            put(path(id))
                .header("Authorization", "Bearer " + replacement)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(newToken)))
        .andExpect(status().isOk());
    var current = devices.findByInstallationId(id).orElseThrow();
    assertThat(current.sessionId()).isNotEqualTo(previousSession);
    assertThat(current.token()).isEqualTo(newToken);
  }

  @Test
  void unregisterIsOwnedAndIdempotent() throws Exception {
    var id = installation();
    var raw = credential();
    mvc.perform(
            put(path(id))
                .header("Authorization", "Bearer " + raw)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID().toString())))
        .andExpect(status().isOk());
    mvc.perform(delete(path(id)).header("Authorization", "Bearer " + raw))
        .andExpect(status().isNoContent());
    mvc.perform(delete(path(id)).header("Authorization", "Bearer " + raw))
        .andExpect(status().isNoContent());
    assertThat(devices.findByInstallationId(id)).isEmpty();
  }

  @Test
  void consentSetupSessionCannotRegisterOrUnregisterDevices() throws Exception {
    var raw = credential();
    var owner = tokens.authenticate(raw).orElseThrow().subjectId();
    var adult =
        links
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    owner, "Ana", LocalDate.of(1950, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    var link =
        links
            .generateLinkingCode(new GenerateLinkingCodeCommand(owner, adult.id()))
            .toOptional()
            .orElseThrow();
    var acceptance =
        mvc.perform(
                post("/api/v1/care-links/acceptances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"caregiverId\":\""
                            + owner
                            + "\",\"code\":\""
                            + link.linkingCode()
                            + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String setup = JsonPath.read(acceptance, "$.accessToken");
    credentials.add(setup);
    var id = installation();
    mvc.perform(
            put(path(id))
                .header("Authorization", "Bearer " + setup)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID().toString())))
        .andExpect(status().isForbidden());
    mvc.perform(delete(path(id)).header("Authorization", "Bearer " + setup))
        .andExpect(status().isForbidden());
    assertThat(devices.findByInstallationId(id)).isEmpty();
  }

  @Test
  void competingSessionsCannotBothClaimTheSameInstallation() throws Exception {
    var id = installation();
    var first = credential();
    var second = credential();
    var ready = new CountDownLatch(2);
    var go = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var responses = new ArrayList<Future<Integer>>();
      for (String raw : List.of(first, second))
        responses.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!go.await(10, TimeUnit.SECONDS))
                    throw new IllegalStateException("request start timed out");
                  return mvc.perform(
                          put(path(id))
                              .header("Authorization", "Bearer " + raw)
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(payload(UUID.randomUUID().toString())))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      go.countDown();
      assertThat(
              List.of(
                  responses.get(0).get(20, TimeUnit.SECONDS),
                  responses.get(1).get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
      assertThat(devices.findByInstallationId(id)).isPresent();
    } finally {
      go.countDown();
    }
  }

  @Test
  void duplicateConflictDoesNotPoisonAnAmbientCallerTransaction() {
    var raw = credential();
    var first = installation();
    var second = installation();
    var token = UUID.randomUUID().toString();
    assertThat(commands.register(new RegisterPushDeviceCommand(first, token), raw).isSuccess())
        .isTrue();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              assertThat(
                      commands
                          .register(new RegisterPushDeviceCommand(second, token), raw)
                          .isFailure())
                  .isTrue();
              assertThat(status.isRollbackOnly()).isFalse();
              assertThat(
                      commands
                          .register(
                              new RegisterPushDeviceCommand(second, UUID.randomUUID().toString()),
                              raw)
                          .isSuccess())
                  .isTrue();
              assertThat(status.isRollbackOnly()).isFalse();
            });
    assertThat(devices.findByInstallationId(first)).isPresent();
    assertThat(devices.findByInstallationId(second)).isPresent();
  }

  @Test
  void dispatchUsesRotatedTokenButNeverRedirectsOriginalSessionWorkAfterReassignment() {
    var raw = credential();
    var id = installation();
    var originalToken = UUID.randomUUID().toString();
    var subject = tokens.authenticate(raw).orElseThrow().subjectId();
    assertThat(commands.register(new RegisterPushDeviceCommand(id, originalToken), raw).isSuccess())
        .isTrue();
    var original = devices.findByInstallationId(id).orElseThrow();
    var rotated = "rotated:" + originalToken;
    assertThat(commands.register(new RegisterPushDeviceCommand(id, rotated), raw).isSuccess())
        .isTrue();
    assertThat(recipientDevices.findCaregiverDevice(id, original.sessionId(), subject))
        .hasValueSatisfying(address -> assertThat(address.token()).isEqualTo(rotated));
    assertThat(recipientDevices.findCaregiverDevice(id, original.sessionId(), "unrelated-subject"))
        .isEmpty();
    assertThat(recipientDevices.findOlderAdultDevice(id, original.sessionId(), subject)).isEmpty();
    tokens.revoke(raw);
    assertThat(recipientDevices.findCaregiverDevice(id, original.sessionId(), subject)).isEmpty();
    var replacement = credential();
    var replacementSubject = tokens.authenticate(replacement).orElseThrow().subjectId();
    assertThat(
            commands
                .register(
                    new RegisterPushDeviceCommand(id, UUID.randomUUID().toString()), replacement)
                .isSuccess())
        .isTrue();
    assertThat(recipientDevices.findCaregiverDevice(id, original.sessionId(), subject)).isEmpty();
    var current = devices.findByInstallationId(id).orElseThrow();
    assertThat(recipientDevices.findCaregiverDevice(id, current.sessionId(), replacementSubject))
        .isPresent();
  }
}
