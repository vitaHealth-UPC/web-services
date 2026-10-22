-- Read-only PostgreSQL diagnostics; returns counts without user references or contact details.
BEGIN TRANSACTION READ ONLY;
SELECT 'incomplete_quiet_hours' AS finding, COUNT(*) AS affected_rows
FROM user_preferences
WHERE (quiet_hours_start IS NULL) <> (quiet_hours_end IS NULL)
UNION ALL
SELECT 'invalid_quiet_hours', COUNT(*) FROM user_preferences
WHERE quiet_hours_start IS NOT NULL AND quiet_hours_end IS NOT NULL
  AND (quiet_hours_start = quiet_hours_end
       OR EXTRACT(SECOND FROM quiet_hours_start) <> 0
       OR EXTRACT(SECOND FROM quiet_hours_end) <> 0)
UNION ALL
SELECT 'incomplete_notification_policy', COUNT(*) FROM user_preferences
WHERE (reminder_minutes IS NOT NULL OR pending_alert_minutes IS NOT NULL
       OR omitted_alert_minutes IS NOT NULL OR escalation_minutes IS NOT NULL
       OR daily_summary_time IS NOT NULL)
  AND (reminder_minutes IS NULL OR pending_alert_minutes IS NULL
       OR omitted_alert_minutes IS NULL OR escalation_minutes IS NULL)
UNION ALL
SELECT 'invalid_notification_policy', COUNT(*) FROM user_preferences
WHERE reminder_minutes IS NOT NULL AND pending_alert_minutes IS NOT NULL
  AND omitted_alert_minutes IS NOT NULL AND escalation_minutes IS NOT NULL
  AND (reminder_minutes < 1 OR reminder_minutes >= pending_alert_minutes
       OR pending_alert_minutes >= omitted_alert_minutes
       OR omitted_alert_minutes >= escalation_minutes OR escalation_minutes > 1440
       OR EXTRACT(SECOND FROM daily_summary_time) <> 0);
ROLLBACK;
