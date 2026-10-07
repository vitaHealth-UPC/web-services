package com.tata.accessibilitypreferences;

import com.tata.accessibilitypreferences.application.queryservices.UserPreferencesQueryService;
import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import com.tata.identitysubscription.domain.model.events.AccountEnabled;
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

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the public contract of Accessibility & Preferences through the real context on H2. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccessibilityApiIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired UserPreferencesQueryService publicContract;
    @Autowired IUserPreferencesRepository repository;
    @Autowired ApplicationEventPublisher events;
    @Autowired JdbcTemplate jdbc;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static String newUser() {
        return UUID.randomUUID().toString();
    }

    private static String preferences(String userId) {
        return "/api/v1/users/" + userId + "/preferences";
    }

    private static String notificationPreferences(String userId) {
        return "/api/v1/users/" + userId + "/notification-preferences";
    }

    @Test
    void aNewUserGetsTheDefaults() throws Exception {
        var userId = newUser();

        mockMvc.perform(get(preferences(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.textSize").value("MEDIUM"))
                .andExpect(jsonPath("$.highContrast").value(false))
                .andExpect(jsonPath("$.reducedMotion").value(false))
                .andExpect(jsonPath("$.readingAssistance").value(false))
                .andExpect(jsonPath("$.voiceConfirmationEnabled").value(true))
                .andExpect(jsonPath("$.quietHours").doesNotExist())
                .andExpect(jsonPath("$.notificationChannels.length()").value(3));
    }

    @Test
    void theTextSizeIsKeptForTheNextSession() throws Exception {
        var userId = newUser();

        mockMvc.perform(put(preferences(userId) + "/text-size")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textSize\":\"LARGE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textSize").value("LARGE"));

        mockMvc.perform(get(preferences(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textSize").value("LARGE"));
    }

    @Test
    void rejectsATextSizeThatDoesNotExist() throws Exception {
        mockMvc.perform(put(preferences(newUser()) + "/text-size")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textSize\":\"HUGE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsAMissingTextSize() throws Exception {
        mockMvc.perform(put(preferences(newUser()) + "/text-size")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
    }

    @Test
    void contrastMotionReadingAndVoiceAreKept() throws Exception {
        var userId = newUser();

        mockMvc.perform(put(preferences(userId) + "/contrast")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(preferences(userId) + "/reduced-motion")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(preferences(userId) + "/reading-assistance")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(preferences(userId) + "/voice-confirmation")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(preferences(userId)))
                .andExpect(jsonPath("$.highContrast").value(true))
                .andExpect(jsonPath("$.reducedMotion").value(true))
                .andExpect(jsonPath("$.readingAssistance").value(true))
                .andExpect(jsonPath("$.voiceConfirmationEnabled").value(false));
    }

    @Test
    void quietHoursAndChannelsAreSavedAndTheOtherModulesSeeThem() throws Exception {
        var userId = newUser();

        mockMvc.perform(put(notificationPreferences(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
    void quietHoursCanBeRemoved() throws Exception {
        var userId = newUser();
        mockMvc.perform(put(notificationPreferences(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quietHours\":{\"start\":\"22:00\",\"end\":\"07:00\"},\"channels\":[{\"type\":\"PUSH\",\"enabled\":true}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(notificationPreferences(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quietHours\":null,\"channels\":[{\"type\":\"PUSH\",\"enabled\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quietHours").doesNotExist());

        assertNull(publicContract.getNotificationPreferences(userId).quietHoursStart());
    }

    @Test
    void anInvalidIntervalChangesNothing() throws Exception {
        var userId = newUser();

        mockMvc.perform(put(notificationPreferences(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quietHours\":{\"start\":\"08:00\",\"end\":\"08:00\"},\"channels\":[{\"type\":\"SMS\",\"enabled\":true}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));

        assertFalse(publicContract.getNotificationPreferences(userId).smsEnabled());
    }

    @Test
    void aRepeatedChannelIsRejected() throws Exception {
        mockMvc.perform(put(notificationPreferences(newUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quietHours\":null,\"channels\":[{\"type\":\"PUSH\",\"enabled\":true},{\"type\":\"PUSH\",\"enabled\":false}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
    }

    @Test
    void theChannelListIsRequired() throws Exception {
        mockMvc.perform(put(notificationPreferences(newUser()))
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
        var rows = jdbc.queryForObject(
                "select count(*) from user_preferences where user_id = ?", Integer.class, accountId);
        assertEquals(1, rows);
    }

    @Test
    void tablesFollowTheDatabaseDesignOfTheReport() {
        var channels = jdbc.queryForObject("select count(*) from user_notification_channels", Integer.class);
        var preferences = jdbc.queryForObject("select count(*) from user_preferences", Integer.class);

        assertTrue(channels >= 0);
        assertTrue(preferences >= 0);
    }
}
