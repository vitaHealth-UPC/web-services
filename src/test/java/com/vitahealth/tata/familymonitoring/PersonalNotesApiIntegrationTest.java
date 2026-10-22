package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.identitysubscription.application.commandservices.PinCommandService;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterPinCommand;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Exercises private-note authorization through real signed sessions and HTTP security filters. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersonalNotesApiIntegrationTest {
  private static final String PATH = "/api/v1/me/notes";
  private static final String NOTE =
      "{\"title\":\" Agua \",\"text\":\" Preparar un vaso \",\"category\":\"ROUTINE\"}";
  @Autowired WebApplicationContext context;
  @Autowired AccountRepository accounts;
  @Autowired PasswordHasher hasher;
  @Autowired SessionTokenService tokens;
  @Autowired CareLinkCommandService links;
  @Autowired PinCommandService pins;
  @Autowired Clock clock;
  private MockMvc mvc;
  private Account caregiver;
  private String caregiverToken;

  private record Adult(String id, String token) {}

  @BeforeEach
  void prepareCaregiver() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    var account =
        Account.register(
            "Caregiver",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            hasher.hash("password123"),
            hasher.hash("123456"),
            clock.instant(),
            Duration.ofMinutes(15));
    account.completeVerification();
    caregiver = accounts.save(account);
    caregiverToken = tokens.issue(caregiver.id()).accessToken();
  }

  private Adult adult(boolean consent) throws Exception {
    var profile =
        links
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    caregiver.id(), "Adult", LocalDate.of(1950, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    var link =
        links
            .generateLinkingCode(new GenerateLinkingCodeCommand(caregiver.id(), profile.id()))
            .toOptional()
            .orElseThrow();
    var acceptance =
        mvc.perform(
                post("/api/v1/care-links/acceptances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"caregiverId\":\""
                            + caregiver.id()
                            + "\",\"code\":\""
                            + link.linkingCode()
                            + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var setup = JsonPath.<String>read(acceptance, "$.accessToken");
    if (!consent) return new Adult(profile.id(), setup);
    mvc.perform(
            post("/api/v1/care-links/{id}/consent", link.id())
                .header("Authorization", "Bearer " + setup)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accepted\":true}"))
        .andExpect(status().isOk());
    pins.register(new RegisterPinCommand(profile.id(), "1234")).toOptional().orElseThrow();
    return new Adult(profile.id(), pinLogin(profile.id()));
  }

  private String pinLogin(String owner) throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/pin-sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"olderAdultId\":\"" + owner + "\",\"pin\":\"1234\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(response, "$.accessToken");
  }

  @Test
  void pinOwnersCannotRedirectPrivateNoteOwnershipThroughSelectors() throws Exception {
    var first = adult(true);
    var other = adult(true);
    mvc.perform(
            post(PATH)
                .header("Authorization", "Bearer " + first.token())
                .param("olderAdultId", other.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NOTE))
        .andExpect(status().isForbidden());
    var before = clock.instant();
    var response =
        mvc.perform(
                post(PATH)
                    .header("Authorization", "Bearer " + first.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(NOTE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.title").value("Agua"))
            .andExpect(jsonPath("$.text").value("Preparar un vaso"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    var recorded = Instant.parse(JsonPath.<String>read(response, "$.recordedAt"));
    assertFalse(recorded.isBefore(before));
    assertFalse(recorded.isAfter(clock.instant()));
    mvc.perform(
            get(PATH)
                .header("Authorization", "Bearer " + first.token())
                .param("olderAdultId", other.id()))
        .andExpect(status().isForbidden());
    mvc.perform(
            get(PATH)
                .header("Authorization", "Bearer " + other.token())
                .param("olderAdultId", first.id()))
        .andExpect(status().isForbidden());
    mvc.perform(get(PATH).header("Authorization", "Bearer " + other.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            post(PATH)
                .header("Authorization", "Bearer " + other.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NOTE.replace("Agua", "Rutina")))
        .andExpect(status().isCreated());
    mvc.perform(get(PATH).header("Authorization", "Bearer " + first.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Agua"))
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get(PATH).header("Authorization", "Bearer " + other.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Rutina"))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void caregiverOnboardingAndAnonymousRequestsCannotReadOrAppendPrivateNotes() throws Exception {
    var onboarding = adult(false);
    assertTrue(tokens.authenticate(onboarding.token()).isPresent());
    for (String token : List.of(caregiverToken, onboarding.token())) {
      mvc.perform(get(PATH).header("Authorization", "Bearer " + token))
          .andExpect(status().isForbidden());
      mvc.perform(
              post(PATH)
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(NOTE))
          .andExpect(status().isForbidden());
    }
    mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(NOTE))
        .andExpect(status().isUnauthorized());
    mvc.perform(get(PATH).header("Authorization", "Bearer invalid"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void validationAndLogoutCannotAppendDataWhileNewPinLoginRetainsHistory() throws Exception {
    var owner = adult(true);
    for (String invalid :
        List.of(
            NOTE.replace(" Agua ", " "),
            NOTE.replace("Preparar un vaso", "x".repeat(1001)),
            NOTE.replace("ROUTINE", "UNKNOWN"))) {
      mvc.perform(
              post(PATH)
                  .header("Authorization", "Bearer " + owner.token())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(invalid))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(get(PATH).header("Authorization", "Bearer " + owner.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            post(PATH)
                .header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NOTE))
        .andExpect(status().isCreated());
    mvc.perform(delete("/api/v1/sessions").header("Authorization", "Bearer " + owner.token()))
        .andExpect(status().isNoContent());
    mvc.perform(get(PATH).header("Authorization", "Bearer " + owner.token()))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post(PATH)
                .header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NOTE))
        .andExpect(status().isUnauthorized());
    mvc.perform(get(PATH).header("Authorization", "Bearer " + pinLogin(owner.id())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("Agua"));
  }
}
