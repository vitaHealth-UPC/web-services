package com.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;

/** Nullable columns allow existing user preference rows to retain product defaults. */
@Embeddable
public class NotificationPolicyEmbeddable {
  @Column(name = "reminder_minutes")
  private Integer reminderMinutes;

  @Column(name = "pending_alert_minutes")
  private Integer pendingAlertMinutes;

  @Column(name = "omitted_alert_minutes")
  private Integer omittedAlertMinutes;

  @Column(name = "escalation_minutes")
  private Integer escalationMinutes;

  @Column(name = "daily_summary_time")
  private LocalTime dailySummaryTime;

  protected NotificationPolicyEmbeddable() {}

  public NotificationPolicyEmbeddable(
      int reminder, int pending, int omitted, int escalation, LocalTime summary) {
    reminderMinutes = reminder;
    pendingAlertMinutes = pending;
    omittedAlertMinutes = omitted;
    escalationMinutes = escalation;
    dailySummaryTime = summary;
  }

  public Integer getReminderMinutes() {
    return reminderMinutes;
  }

  public Integer getPendingAlertMinutes() {
    return pendingAlertMinutes;
  }

  public Integer getOmittedAlertMinutes() {
    return omittedAlertMinutes;
  }

  public Integer getEscalationMinutes() {
    return escalationMinutes;
  }

  public LocalTime getDailySummaryTime() {
    return dailySummaryTime;
  }
}
