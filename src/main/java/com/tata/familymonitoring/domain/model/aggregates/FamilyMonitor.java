package com.tata.familymonitoring.domain.model.aggregates;

import com.tata.familymonitoring.domain.exceptions.AlertNotFoundException;
import com.tata.familymonitoring.domain.model.entities.AdherenceInsight;
import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.tata.familymonitoring.domain.model.entities.LowStockNotice;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Active follow-up of a caregiver over an older adult. The care link, the older adult and the
 * caregiver are referenced by logical id only.
 */
@Entity
public class FamilyMonitor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 36)
  private String careLinkId;

  @Column(nullable = false, length = 36)
  private String olderAdultId;

  @Column(nullable = false, length = 36)
  private String familiarId;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "family_monitor_id")
  private List<AlertSummary> alerts = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "family_monitor_id")
  private List<CaregiverNote> notes = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "family_monitor_id")
  private List<LowStockNotice> lowStockNotices = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "family_monitor_id")
  private List<AdherenceInsight> adherenceInsights = new ArrayList<>();

  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  protected FamilyMonitor() {
  }

  public FamilyMonitor(String careLinkId, String olderAdultId, String familiarId) {
    this.careLinkId = careLinkId;
    this.olderAdultId = olderAdultId;
    this.familiarId = familiarId;
  }

  /** Adds the alert for an omitted intake. If the intake already has one, nothing changes. */
  public AlertSummary addAlert(
      String intakeId, String medicationName, Instant scheduledAt, String reason, Instant now) {
    return alerts.stream()
        .filter(alert -> alert.getIntakeId().equals(intakeId))
        .findFirst()
        .orElseGet(() -> {
          AlertSummary alert = new AlertSummary(intakeId, medicationName, scheduledAt, reason, now);
          alerts.add(alert);
          return alert;
        });
  }

  public AlertSummary markAlertAttended(Long alertId) {
    AlertSummary alert = findAlert(alertId);
    alert.markAttended();
    return alert;
  }

  public AlertSummary closeAlert(Long alertId, Instant now) {
    AlertSummary alert = findAlert(alertId);
    alert.close(now);
    return alert;
  }

  public CaregiverNote addNote(String text, String familiarId, Instant now) {
    CaregiverNote note = new CaregiverNote(text, familiarId, now);
    notes.add(note);
    return note;
  }

  public AlertSummary findAlert(Long alertId) {
    return alerts.stream()
        .filter(alert -> alertId.equals(alert.getId()))
        .findFirst()
        .orElseThrow(() -> new AlertNotFoundException(alertId));
  }

  public List<AlertSummary> openAlerts() {
    return alerts.stream().filter(AlertSummary::isOpen).toList();
  }

  public boolean hasOpenAlert() {
    return alerts.stream().anyMatch(AlertSummary::isOpen);
  }

  /** Marks the medication as low on stock. A second notice for the same medication only refreshes it. */
  public LowStockNotice registerLowStock(
      String medicationId, String medicationName, int remainingStock, int replenishmentThreshold, Instant now) {
    var existing = lowStockNotices.stream()
        .filter(notice -> notice.getMedicationId().equals(medicationId))
        .findFirst();
    if (existing.isPresent()) {
      existing.get().refresh(medicationName, remainingStock, replenishmentThreshold, now);
      return existing.get();
    }
    var notice = new LowStockNotice(medicationId, medicationName, remainingStock, replenishmentThreshold, now);
    lowStockNotices.add(notice);
    return notice;
  }

  /** Clears the low-stock notice once a replenishment leaves the stock above its threshold. */
  public void resolveLowStock(String medicationId, int remainingStock) {
    lowStockNotices.removeIf(
        notice -> notice.getMedicationId().equals(medicationId)
            && remainingStock > notice.getReplenishmentThreshold());
  }

  /** Keeps the insight once; the same pattern reported again changes nothing. */
  public AdherenceInsight registerInsight(
      String medicationId, String medicationName, int omissionDays, LocalDate firstDay, LocalDate lastDay,
      Instant now) {
    return adherenceInsights.stream()
        .filter(insight -> insight.isSamePattern(medicationId, firstDay, lastDay))
        .findFirst()
        .orElseGet(() -> {
          var insight = new AdherenceInsight(medicationId, medicationName, omissionDays, firstDay, lastDay, now);
          adherenceInsights.add(insight);
          return insight;
        });
  }

  public boolean hasLowStock() {
    return !lowStockNotices.isEmpty();
  }

  /** Most recent first. */
  public List<AdherenceInsight> recentInsights(int limit) {
    return adherenceInsights.stream()
        .sorted(Comparator.comparing(AdherenceInsight::getDetectedAt).reversed())
        .limit(limit)
        .toList();
  }

  public CaregiverNote latestNote() {
    return notes.getLast();
  }

  public Long getId() {
    return id;
  }

  public String getCareLinkId() {
    return careLinkId;
  }

  public String getOlderAdultId() {
    return olderAdultId;
  }

  public String getFamiliarId() {
    return familiarId;
  }

  public List<AlertSummary> getAlerts() {
    return Collections.unmodifiableList(alerts);
  }

  public List<CaregiverNote> getNotes() {
    return Collections.unmodifiableList(notes);
  }

  public List<LowStockNotice> getLowStockNotices() {
    return Collections.unmodifiableList(lowStockNotices);
  }
}
