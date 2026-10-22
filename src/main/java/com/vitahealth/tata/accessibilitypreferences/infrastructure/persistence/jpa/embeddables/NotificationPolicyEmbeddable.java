package com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;

/**
 * Stores timing columns; only an entirely absent legacy policy is eligible for product defaults.
 */
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

  /** Required by JPA; reconstruction distinguishes absent and partially stored policies. */
  protected NotificationPolicyEmbeddable() {}

  /** Copies the domain-validated delays and nullable summary time without reinterpreting them. */
  public NotificationPolicyEmbeddable(
      int reminder, int pending, int omitted, int escalation, LocalTime summary) {
    reminderMinutes = reminder;
    pendingAlertMinutes = pending;
    omittedAlertMinutes = omitted;
    escalationMinutes = escalation;
    dailySummaryTime = summary;
  }

  /** Returns the initial reminder delay, nullable only in legacy or inconsistent stored data. */
  public Integer getReminderMinutes() {
    return reminderMinutes;
  }

  /** Returns the pending-alert delay measured from the scheduled intake. */
  public Integer getPendingAlertMinutes() {
    return pendingAlertMinutes;
  }

  /** Returns the configured omitted-alert delay. */
  public Integer getOmittedAlertMinutes() {
    return omittedAlertMinutes;
  }

  /** Returns the configured escalation delay. */
  public Integer getEscalationMinutes() {
    return escalationMinutes;
  }

  /** Returns the summary time; null disables it when the four delays are present. */
  public LocalTime getDailySummaryTime() {
    return dailySummaryTime;
  }
}
