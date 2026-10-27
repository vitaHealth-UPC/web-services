package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.SocialIdentityVerifier;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerifiedSocialIdentity;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerifiedSocialIdentity.Provider;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/** End-to-end Tata session issuance from verified social evidence; no external Firebase calls. */
@SpringBootTest
@ActiveProfiles("test")
class SocialSessionApiIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired AccountRepository accounts;
  @Autowired AccountJpaRepository accountRows;
  @Autowired SocialAccountLinkJpaRepository socialRows;
  @Autowired AccountSessionJpaRepository sessionRows;
  @Autowired SessionTokenService sessions;
  @Autowired PasswordHasher hasher;
  @Autowired PlatformTransactionManager manager;
  @Autowired Clock clock;
  @MockitoBean SocialIdentityVerifier verifier;

  private MockMvc mvc;
  private final Set<String> ids = new HashSet<>();
  private final Set<String> usedEmails = new HashSet<>();

  @BeforeEach void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterEach void cleanup() {
    new TransactionTemplate(manager).executeWithoutResult(status -> {
      for (var id : ids) {
        sessionRows.revokeCaregiverSessions(id);
        socialRows.deleteByAccountId(id);
        accountRows.deleteById(id);
      }
      for (var email : usedEmails) {
        var account = accountRows.findByEmail(email);
        if (account.isPresent()) {
          var id = account.get().getId();
          sessionRows.revokeCaregiverSessions(id);
          socialRows.deleteByAccountId(id);
          accountRows.deleteById(id);
        }
      }
    });
    reset(verifier);
  }

  private Account createExistingAccount() {
    var account = Account.register(
        "Existing Caregiver",
        new EmailAddress(UUID.randomUUID() + "@example.com"),
        hasher.hash("some-password"),
        "verification", clock.instant(), Duration.ofMinutes(15));
    account.completeVerification();
    account = accounts.save(account);
    ids.add(account.id());
    return account;
  }

  private VerifiedSocialIdentity identity(String uid, String email, boolean verified) {
    return new VerifiedSocialIdentity(
        "https://securetoken.google.com/tata-project", uid, Provider.GOOGLE, email, verified, "Caregiver");
  }

  private static String body(String credential) {
    return "{\"firebaseIdToken\":\"" + credential + "\"}";
  }

  @Test void firstVerifiedIdentityCreatesTataAccountAndSubsequentLoginReturnsSameOwner() throws Exception {
    var email = UUID.randomUUID() + "@example.com";
    usedEmails.add(email);
    when(verifier.verify("valid-first")).thenReturn(identity("new-uid-"+UUID.randomUUID(), email, true));
    var first = mvc.perform(post("/api/v1/social-sessions")
        .contentType(MediaType.APPLICATION_JSON).content(body("valid-first")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").exists())
        .andExpect(jsonPath("$.accessToken").exists())
        .andReturn();
    var id = com.jayway.jsonpath.JsonPath.read(first.getResponse().getContentAsString(), "$.accountId").toString();
    ids.add(id);
    var account = accounts.findById(id).orElseThrow();
    assertTrue(account.canAuthenticate());
    assertEquals(email, account.email().value());
    // The generated unusable password must not accidentally authenticate as a normal user.
    mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + email + "\",\"password\":\"guess\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/social-sessions")
        .contentType(MediaType.APPLICATION_JSON).content(body("valid-first")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").value(id));
  }

  @Test void existingEmailCannotBeTakenOverByMatchingProviderEmailAndMayBeLinkedAfterTataLogin()
      throws Exception {
    var existing = createExistingAccount();
    var social = identity("existing-uid-"+UUID.randomUUID(), existing.email().value(), true);
    when(verifier.verify("existing-social")).thenReturn(social);
    mvc.perform(post("/api/v1/social-sessions")
        .contentType(MediaType.APPLICATION_JSON).content(body("existing-social")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("SOCIAL_LINK_REQUIRED"));
    mvc.perform(put("/api/v1/me/social-identities")
        .contentType(MediaType.APPLICATION_JSON).content(body("existing-social")))
        .andExpect(status().isUnauthorized());
    var tataToken = sessions.issue(existing.id()).accessToken();
    mvc.perform(put("/api/v1/me/social-identities")
        .header("Authorization", "Bearer " + tataToken)
        .contentType(MediaType.APPLICATION_JSON).content(body("existing-social")))
        .andExpect(status().isNoContent());
    mvc.perform(put("/api/v1/me/social-identities")
        .header("Authorization", "Bearer " + tataToken)
        .contentType(MediaType.APPLICATION_JSON).content(body("existing-social")))
        .andExpect(status().isNoContent());
    mvc.perform(post("/api/v1/social-sessions")
        .contentType(MediaType.APPLICATION_JSON).content(body("existing-social")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").value(existing.id()));

    var other = createExistingAccount();
    var otherToken = sessions.issue(other.id()).accessToken();
    mvc.perform(put("/api/v1/me/social-identities")
        .header("Authorization", "Bearer " + otherToken)
        .contentType(MediaType.APPLICATION_JSON).content(body("existing-social")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("SOCIAL_LINK_CONFLICT"));
  }

  @Test void invalidUnverifiedAndAbsentEmailEvidenceCannotCreateAccount() throws Exception {
    when(verifier.verify("invalid")).thenThrow(new IllegalArgumentException("Invalid social credential"));
    mvc.perform(post("/api/v1/social-sessions")
        .contentType(MediaType.APPLICATION_JSON).content(body("invalid")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("INVALID_SOCIAL_CREDENTIAL"));
    when(verifier.verify("unverified")).thenReturn(identity("no-verified", "unknown@example.com", false));
    mvc.perform(post("/api/v1/social-sessions")
        .contentType(MediaType.APPLICATION_JSON).content(body("unverified")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.title").value("SOCIAL_EMAIL_REQUIRED"));
    when(verifier.verify("no-email")).thenReturn(identity("missing-email", null, false));
    mvc.perform(post("/api/v1/social-sessions")
        .contentType(MediaType.APPLICATION_JSON).content(body("no-email")))
        .andExpect(status().isUnprocessableEntity());
  }
}
