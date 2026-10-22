package com.vitahealth.tata.accessibilitypreferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.accessibilitypreferences.application.queryservices.GetUserPreferencesQueryService;
import com.vitahealth.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.vitahealth.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationChannel;
import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.NotificationPolicy;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies real stored configuration is diagnosed through public reads without altering evidence.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PreferencesStoredEvidenceIntegrationTest {
  @Autowired UserPreferencesRepository repository;
  @Autowired GetUserPreferencesQueryService query;
  @Autowired JdbcTemplate jdbc;
  @Autowired EntityManager entities;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "quiet_start_only", "quiet_end_only", "quiet_seconds", "missing_reminder",
        "missing_pending", "missing_omitted", "missing_escalation", "summary_only",
        "policy_order", "summary_seconds"
      })
  void incompleteOrInvalidRowsRemainTechnicalFailuresWithoutRepair(String corruption) {
    var user = seed();
    var changes =
        switch (corruption) {
          case "quiet_start_only" -> "quiet_hours_start = TIME '22:00', quiet_hours_end = NULL";
          case "quiet_end_only" -> "quiet_hours_start = NULL, quiet_hours_end = TIME '07:00'";
          case "quiet_seconds" ->
              "quiet_hours_start = TIME '22:00:01', quiet_hours_end = TIME '07:00'";
          case "missing_reminder" -> "reminder_minutes = NULL";
          case "missing_pending" -> "pending_alert_minutes = NULL";
          case "missing_omitted" -> "omitted_alert_minutes = NULL";
          case "missing_escalation" -> "escalation_minutes = NULL";
          case "summary_only" ->
              "reminder_minutes = NULL, pending_alert_minutes = NULL, omitted_alert_minutes = NULL,"
                  + " escalation_minutes = NULL";
          case "policy_order" -> "reminder_minutes = 20, pending_alert_minutes = 10";
          case "summary_seconds" -> "daily_summary_time = TIME '20:00:01'";
          default -> throw new IllegalArgumentException("unknown test case");
        };
    // The assignments are fixed test cases; the user reference remains a bound SQL parameter.
    jdbc.update("update user_preferences set " + changes + " where user_id = ?", user);
    entities.clear();
    var before = jdbc.queryForMap("select * from user_preferences where user_id = ?", user);

    var failure =
        assertThrows(RuntimeException.class, () -> query.handle(new GetUserPreferencesQuery(user)));
    Throwable origin = failure;
    while (origin.getCause() != null && !(origin instanceof IllegalStateException))
      origin = origin.getCause();
    assertInstanceOf(IllegalStateException.class, origin);
    assertEquals("stored user preferences are inconsistent", origin.getMessage());
    entities.clear();
    assertEquals(
        before, jdbc.queryForMap("select * from user_preferences where user_id = ?", user));
  }

  @Test
  void entirelyAbsentLegacyPolicyReadsDefaultsWithoutSavingThem() {
    var user = seed();
    jdbc.update(
        "update user_preferences set reminder_minutes = NULL, pending_alert_minutes = NULL,"
            + " omitted_alert_minutes = NULL, escalation_minutes = NULL, daily_summary_time = NULL"
            + " where user_id = ?",
        user);
    entities.clear();
    var before = jdbc.queryForMap("select * from user_preferences where user_id = ?", user);
    var result = query.handle(new GetUserPreferencesQuery(user));
    assertEquals(NotificationPolicy.defaults(), result.notificationPolicy());
    assertEquals(Instant.EPOCH, result.createdAt());
    entities.clear();
    assertEquals(
        before, jdbc.queryForMap("select * from user_preferences where user_id = ?", user));
  }

  @Test
  void completePolicyRetainsDisabledSummaryWithoutFillingIt() {
    var user = seed();
    jdbc.update("update user_preferences set daily_summary_time = NULL where user_id = ?", user);
    entities.clear();
    var before = jdbc.queryForMap("select * from user_preferences where user_id = ?", user);
    var result = query.handle(new GetUserPreferencesQuery(user));
    assertNull(result.notificationPolicy().dailySummaryTime());
    assertEquals(10, result.notificationPolicy().reminderMinutes());
    entities.clear();
    assertEquals(
        before, jdbc.queryForMap("select * from user_preferences where user_id = ?", user));
  }

  private String seed() {
    var user = UUID.randomUUID().toString();
    repository.save(UserPreferencesFactory.createDefaults(user, Instant.EPOCH));
    entities.flush();
    return user;
  }

  @Test
  void legacyThreeChannelListIsNotExpandedDuringARead() {
    var user = UUID.randomUUID().toString();
    var legacy = UserPreferencesFactory.createDefaults(user, Instant.EPOCH);
    legacy.updateChannels(
        List.of(
            new NotificationChannel(ChannelType.PUSH, true),
            new NotificationChannel(ChannelType.SMS, false),
            new NotificationChannel(ChannelType.EMAIL, false)));
    repository.save(legacy);
    entities.flush();
    entities.clear();
    var result = query.handle(new GetUserPreferencesQuery(user));
    assertEquals(3, result.notificationChannels().size());
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from user_notification_channels where user_preferences_id = ? and"
                + " channel_type = 'CALL'",
            Integer.class,
            legacy.id()));
  }
}
