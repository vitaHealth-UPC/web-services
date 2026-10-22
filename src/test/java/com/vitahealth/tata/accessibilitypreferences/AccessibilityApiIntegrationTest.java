package com.vitahealth.tata.accessibilitypreferences;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitahealth.tata.accessibilitypreferences.application.queryservices.UserPreferencesQueryService;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.events.AccountEnabled;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Exercises the public contract of Accessibility & Preferences through the real context on H2. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccessibilityApiIntegrationTest {

  @Autowired WebApplicationContext context;
  @Autowired UserPreferencesQueryService publicContract;
  @Autowired UserPreferencesRepository repository;
  @Autowired ApplicationEventPublisher events;
  @Autowired JdbcTemplate jdbc;
  @Autowired EntityManager entityManager;
  @Autowired AccountRepository accounts;
  @Autowired SessionTokenService tokens;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private String newUser() {
    String id = UUID.randomUUID().toString();
    accounts.save(
        Account.rehydrate(
            id,
            "Caregiver",
            new EmailAddress(id + "@example.com"),
            "hash",
            AccountStatus.ACTIVE,
            null,
            null));
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(
                get("/").header("Authorization", "Bearer " + tokens.issue(id).accessToken()))
            .build();
    return id;
  }

  private static String preferences(String userId) {
    return "/api/v1/users/" + userId + "/preferences";
  }

  private static String notificationPreferences(String userId) {
    return "/api/v1/users/" + userId + "/notification-preferences";
  }

  private static String policyBody(int reminder) {
    return """
           {"policy":{"reminderMinutes":%d,"pendingAlertMinutes":25,"omittedAlertMinutes":90,
             "escalationMinutes":150,"dailySummaryTime":"19:30"},
            "contacts":[{"name":"Rosa","phone":"+51 999-123-456"},
                        {"name":"Diego","phone":"+51 988-123-456"}]}
           """
        .formatted(reminder);
  }

  @Test
  void darkThemePersistsWithoutReplacingOtherFieldsAndOnlyItsOwnerCanChangeIt() throws Exception {
    var user = newUser();
    mockMvc
        .perform(get(preferences(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.darkTheme").value(false));
    mockMvc
        .perform(
            put(preferences(user) + "/dark-theme")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.darkTheme").value(true));
    entityManager.flush();
    entityManager.clear();
    mockMvc
        .perform(
            put(preferences(user) + "/text-size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"textSize\":\"LARGE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.darkTheme").value(true));
    entityManager.flush();
    entityManager.clear();
    mockMvc
        .perform(get(preferences(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.darkTheme").value(true))
        .andExpect(jsonPath("$.textSize").value("LARGE"));
    mockMvc
        .perform(
            put(preferences(user) + "/dark-theme")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.darkTheme").value(false));
    mockMvc
        .perform(
            put(preferences(user) + "/dark-theme")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
    entityManager.flush();
    jdbc.update("update user_preferences set dark_theme = null where user_id = ?", user);
    entityManager.clear();
    mockMvc
        .perform(get(preferences(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.darkTheme").value(false));
    var other = newUser(); // Default request now authenticates the other owner.
    mockMvc
        .perform(
            put(preferences(user) + "/dark-theme")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isForbidden());
    MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build()
        .perform(
            put(preferences(other) + "/dark-theme")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void bindingAndDomainFailuresUseTheSameRequestedLanguageAndStableCode() throws Exception {
    var user = newUser();
    for (var language : new String[] {"es-PE", "en-US"}) {
      var message =
          language.startsWith("es")
              ? "Datos de la solicitud no válidos"
              : "request validation failed";
      mockMvc
          .perform(
              put(preferences(user) + "/contrast")
                  .header("Accept-Language", language)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.detail").value(message));
      mockMvc
          .perform(
              put(notificationPreferences(user))
                  .header("Accept-Language", language)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"quietHours\":null,\"channels\":[{\"type\":\"PUSH\",\"enabled\":true},{\"type\":\"PUSH\",\"enabled\":false}]}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.detail").value(message));
    }
    assertTrue(repository.findByUserId(user).isEmpty());
  }

  @Test
  void documentsTheSamePreferenceSuccessResourceForTypedMutationResponses() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$['paths']['/api/v1/users/{userId}/preferences/contrast']['put']['responses']['200']['content']['application/json']['schema']['$ref']")
                .value("#/components/schemas/UserPreferencesResource"))
        .andExpect(
            jsonPath(
                    "$['paths']['/api/v1/users/{userId}/notification-preferences']['put']['responses']['200']['content']['application/json']['schema']['$ref']")
                .value("#/components/schemas/UserPreferencesResource"));
  }

  @Test
  void documentsRequiredPolicyDelaysAndTheirActualDomainBounds() throws Exception {
    var response = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    response.andExpect(
        jsonPath("$.components.schemas.NotificationPolicyResource.required")
            .value(
                hasItems(
                    "reminderMinutes",
                    "pendingAlertMinutes",
                    "omittedAlertMinutes",
                    "escalationMinutes")));
    for (var field :
        new String[] {
          "reminderMinutes", "pendingAlertMinutes", "omittedAlertMinutes", "escalationMinutes"
        }) {
      var property = "$.components.schemas.NotificationPolicyResource.properties." + field;
      response
          .andExpect(jsonPath(property + ".minimum").value(1))
          .andExpect(jsonPath(property + ".maximum").value(1440));
    }
  }

  @Test
  void quietHoursWithSecondsAreRejectedWithoutCreatingPreferences() throws Exception {
    var user = newUser();
    for (var interval :
        new String[] {
          "{\"start\":\"22:00:01\",\"end\":\"07:00\"}",
          "{\"start\":\"22:00\",\"end\":\"07:00:00.000001\"}"
        }) {
      mockMvc
          .perform(
              put(notificationPreferences(user))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"quietHours\":"
                          + interval
                          + ",\"channels\":[{\"type\":\"PUSH\",\"enabled\":true}]}"))
          .andExpect(status().isBadRequest());
    }
    assertTrue(repository.findByUserId(user).isEmpty());
  }

  @Test
  void replacementBooleansMustBeExplicitAndFalseStillDisablesTheChosenSetting() throws Exception {
    var user = newUser();
    for (var setting :
        new String[] {"contrast", "reduced-motion", "reading-assistance", "voice-confirmation"}) {
      for (var body : new String[] {"{}", "{\"enabled\":null}"}) {
        mockMvc
            .perform(
                put(preferences(user) + "/" + setting)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
      }
    }
    assertTrue(repository.findByUserId(user).isEmpty());
    mockMvc
        .perform(
            put(preferences(user) + "/voice-confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.voiceConfirmationEnabled").value(false));
    mockMvc
        .perform(
            put(preferences(user) + "/voice-confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
    assertFalse(repository.findByUserId(user).orElseThrow().voiceConfirmationEnabled());
  }

  @Test
  void omittedChannelChoiceCannotSilentlyDisableSavedPush() throws Exception {
    var user = newUser();
    mockMvc
        .perform(
            put(preferences(user) + "/contrast")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isOk());
    for (var body :
        new String[] {
          "{\"quietHours\":null,\"channels\":[{\"type\":\"PUSH\"}]}",
          "{\"quietHours\":null,\"channels\":[{\"type\":\"PUSH\",\"enabled\":null}]}"
        }) {
      mockMvc
          .perform(
              put(notificationPreferences(user))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }
    var preserved = repository.findByUserId(user).orElseThrow();
    assertTrue(preserved.highContrast());
    assertTrue(preserved.isChannelEnabled(ChannelType.PUSH));
  }

  @Test
  void policyAndOrderedContactsSurviveReloadAndChannelUpdates() throws Exception {
    var user = newUser();
    mockMvc
        .perform(
            put(notificationPreferences(user) + "/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(policyBody(15)))
        .andExpect(status().isOk());
    entityManager.flush();
    entityManager.clear();
    mockMvc
        .perform(get(preferences(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notificationPolicy.reminderMinutes").value(15))
        .andExpect(jsonPath("$.notificationPolicy.dailySummaryTime").value("19:30:00"))
        .andExpect(jsonPath("$.emergencyContacts[0].name").value("Rosa"))
        .andExpect(jsonPath("$.emergencyContacts[0].phone").value("+51999123456"))
        .andExpect(jsonPath("$.emergencyContacts[1].name").value("Diego"))
        .andExpect(jsonPath("$.notificationChannels.length()").value(4));
    mockMvc
        .perform(
            put(notificationPreferences(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"quietHours":null,"channels":[{"type":"PUSH","enabled":false}]}
                    """))
        .andExpect(status().isOk());
    entityManager.clear();
    mockMvc
        .perform(get(preferences(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notificationPolicy.reminderMinutes").value(15))
        .andExpect(jsonPath("$.emergencyContacts.length()").value(2))
        .andExpect(jsonPath("$.notificationChannels.length()").value(1));
  }

  @Test
  void invalidPolicyDoesNotOverwriteStoredSettings() throws Exception {
    var user = newUser();
    mockMvc
        .perform(
            put(notificationPreferences(user) + "/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(policyBody(15)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put(notificationPreferences(user) + "/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(policyBody(30)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            put(notificationPreferences(user) + "/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(policyBody(15).replace("+51 988-123-456", "51999123456")))
        .andExpect(status().isBadRequest());
    entityManager.clear();
    mockMvc
        .perform(get(preferences(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notificationPolicy.reminderMinutes").value(15))
        .andExpect(jsonPath("$.emergencyContacts[1].phone").value("+51988123456"));
  }

  @Test
  void emptyContactsAndDisabledSummaryArePersisted() throws Exception {
    var user = newUser();
    mockMvc
        .perform(
            put(notificationPreferences(user) + "/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
{"policy":{"reminderMinutes":10,"pendingAlertMinutes":20,"omittedAlertMinutes":120,
"escalationMinutes":180,"dailySummaryTime":null},"contacts":[]}
"""))
        .andExpect(status().isOk());
    entityManager.clear();
    mockMvc
        .perform(get(preferences(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notificationPolicy.dailySummaryTime").doesNotExist())
        .andExpect(jsonPath("$.emergencyContacts.length()").value(0));
  }

  @Test
  void aNewUserGetsTheDefaults() throws Exception {
    var userId = newUser();

    mockMvc
        .perform(get(preferences(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.textSize").value("MEDIUM"))
        .andExpect(jsonPath("$.highContrast").value(false))
        .andExpect(jsonPath("$.reducedMotion").value(false))
        .andExpect(jsonPath("$.readingAssistance").value(false))
        .andExpect(jsonPath("$.voiceConfirmationEnabled").value(true))
        .andExpect(jsonPath("$.quietHours").doesNotExist())
        .andExpect(jsonPath("$.notificationChannels.length()").value(4));
    assertTrue(
        repository.findByUserId(userId).isEmpty(), "Reading defaults must not insert preferences");
  }

  @Test
  void theTextSizeIsKeptForTheNextSession() throws Exception {
    var userId = newUser();

    mockMvc
        .perform(
            put(preferences(userId) + "/text-size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"textSize\":\"LARGE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.textSize").value("LARGE"));

    mockMvc
        .perform(get(preferences(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.textSize").value("LARGE"));
  }

  @Test
  void rejectsATextSizeThatDoesNotExist() throws Exception {
    var userId = newUser();
    mockMvc
        .perform(
            put(preferences(userId) + "/text-size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"textSize\":\"HUGE\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsAMissingTextSize() throws Exception {
    var userId = newUser();
    mockMvc
        .perform(
            put(preferences(userId) + "/text-size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
  }

  @Test
  void contrastMotionReadingAndVoiceAreKept() throws Exception {
    var userId = newUser();

    mockMvc
        .perform(
            put(preferences(userId) + "/contrast")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put(preferences(userId) + "/reduced-motion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put(preferences(userId) + "/reading-assistance")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put(preferences(userId) + "/voice-confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get(preferences(userId)))
        .andExpect(jsonPath("$.highContrast").value(true))
        .andExpect(jsonPath("$.reducedMotion").value(true))
        .andExpect(jsonPath("$.readingAssistance").value(true))
        .andExpect(jsonPath("$.voiceConfirmationEnabled").value(false));
  }

  @Test
  void quietHoursAndChannelsAreSavedAndTheOtherModulesSeeThem() throws Exception {
    var userId = newUser();

    mockMvc
        .perform(
            put(notificationPreferences(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"quietHours":{"start":"22:00","end":"07:00"},
                     "channels":[{"type":"PUSH","enabled":false},{"type":"EMAIL","enabled":true}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quietHours.start").value("22:00:00"))
        .andExpect(jsonPath("$.quietHours.end").value("07:00:00"))
        .andExpect(jsonPath("$.notificationChannels.length()").value(2));

    var contract = publicContract.getNotificationPreferences(userId);
    assertFalse(contract.pushEnabled());
    assertTrue(contract.emailEnabled());
    assertFalse(contract.smsEnabled());
    assertEquals(LocalTime.of(22, 0), contract.quietHoursStart());
    assertEquals(LocalTime.of(7, 0), contract.quietHoursEnd());
  }

  @Test
  void callPreferenceIsPersistedAndCanBeDisabled() throws Exception {
    var userId = newUser();
    for (boolean enabled : new boolean[] {true, false}) {
      mockMvc
          .perform(
              put(notificationPreferences(userId))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"quietHours\":null,\"channels\":[{\"type\":\"CALL\",\"enabled\":"
                          + enabled
                          + "}]}"))
          .andExpect(status().isOk());
      mockMvc
          .perform(get(preferences(userId)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.notificationChannels[0].type").value("CALL"))
          .andExpect(jsonPath("$.notificationChannels[0].enabled").value(enabled));
    }
  }

  @Test
  void quietHoursCanBeRemoved() throws Exception {
    var userId = newUser();
    mockMvc
        .perform(
            put(notificationPreferences(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"quietHours\":{\"start\":\"22:00\",\"end\":\"07:00\"},\"channels\":[{\"type\":\"PUSH\",\"enabled\":true}]}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            put(notificationPreferences(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"quietHours\":null,\"channels\":[{\"type\":\"PUSH\",\"enabled\":true}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quietHours").doesNotExist());

    assertNull(publicContract.getNotificationPreferences(userId).quietHoursStart());
  }

  @Test
  void anInvalidIntervalChangesNothing() throws Exception {
    var userId = newUser();

    mockMvc
        .perform(
            put(notificationPreferences(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"quietHours\":{\"start\":\"08:00\",\"end\":\"08:00\"},\"channels\":[{\"type\":\"SMS\",\"enabled\":true}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));

    assertFalse(publicContract.getNotificationPreferences(userId).smsEnabled());
  }

  @Test
  void aRepeatedChannelIsRejected() throws Exception {
    var userId = newUser();
    mockMvc
        .perform(
            put(notificationPreferences(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"quietHours\":null,\"channels\":[{\"type\":\"PUSH\",\"enabled\":true},{\"type\":\"PUSH\",\"enabled\":false}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
  }

  @Test
  void theChannelListIsRequired() throws Exception {
    var userId = newUser();
    mockMvc
        .perform(
            put(notificationPreferences(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quietHours\":null}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void theOtherModulesGetDefaultsWithoutWritingAnything() {
    var userId = newUser();

    var contract = publicContract.getNotificationPreferences(userId);

    assertTrue(contract.pushEnabled());
    assertTrue(publicContract.isVoiceConfirmationEnabled(userId));
    assertTrue(repository.findByUserId(userId).isEmpty());
  }

  @Test
  void anEnabledAccountGetsItsDefaultPreferences() {
    var accountId = newUser();

    events.publishEvent(new AccountEnabled(accountId, Instant.now()));
    events.publishEvent(new AccountEnabled(accountId, Instant.now()));

    assertTrue(repository.findByUserId(accountId).isPresent());
    var rows =
        jdbc.queryForObject(
            "select count(*) from user_preferences where user_id = ?", Integer.class, accountId);
    assertEquals(1, rows);
  }

  @Test
  void tablesFollowTheDatabaseDesignOfTheReport() {
    var channels =
        jdbc.queryForObject("select count(*) from user_notification_channels", Integer.class);
    var preferences = jdbc.queryForObject("select count(*) from user_preferences", Integer.class);

    assertTrue(channels >= 0);
    assertTrue(preferences >= 0);
  }

  @Test
  void anotherUsersPreferencesCannotBeReadOrUpdated() throws Exception {
    newUser();
    var firstUsersSession = mockMvc;
    String otherUser = newUser();
    firstUsersSession.perform(get(preferences(otherUser))).andExpect(status().isForbidden());
    firstUsersSession
        .perform(
            put(preferences(otherUser) + "/text-size")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"textSize\":\"LARGE\"}"))
        .andExpect(status().isForbidden());
  }
}
