package com.vitahealth.tata.familymonitoring.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.familymonitoring.domain.exceptions.AlertNotFoundException;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AdherenceInsight;
import com.vitahealth.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.vitahealth.tata.familymonitoring.domain.model.entities.LowStockNotice;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.InventoryStockPosition;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Follow-up history for an exact caregiver-adult relationship. The care link, older adult and
 * caregiver are referenced by logical id only.
 */
public class FamilyMonitor {

  private Long id;

  private Long revision;

  private final String careLinkId;

  private final String olderAdultId;

  private final String familiarId;

  private final List<AlertSummary> alerts = new ArrayList<>();

  private final List<CaregiverNote> notes = new ArrayList<>();

  private final List<LowStockNotice> lowStockNotices = new ArrayList<>();

  private final List<AdherenceInsight> adherenceInsights = new ArrayList<>();

  private final List<InventoryStockPosition> inventoryStockPositions = new ArrayList<>();

  private Instant createdAt;

  private Instant updatedAt;

  /** Starts unsaved follow-up for one caregiver and the exact care relationship. */
  public FamilyMonitor(String careLinkId, String olderAdultId, String familiarId) {
    this.careLinkId = requireText(careLinkId, "careLinkId");
    this.olderAdultId = requireText(olderAdultId, "olderAdultId");
    this.familiarId = requireText(familiarId, "familiarId");
  }

  /** Adds the alert for an omitted intake. If the intake already has one, nothing changes. */
  public AlertSummary addAlert(
      String intakeId, String medicationName, Instant scheduledAt, String reason, Instant now) {
    return alerts.stream()
        .filter(alert -> alert.getIntakeId().equals(intakeId))
        .findFirst()
        .orElseGet(
            () -> {
              AlertSummary alert =
                  new AlertSummary(intakeId, medicationName, scheduledAt, reason, now);
              alerts.add(alert);
              return alert;
            })
        .snapshot();
  }

  /** Records caregiver attention while retaining the alert as pending until closure. */
  public AlertSummary markAlertAttended(Long alertId) {
    AlertSummary alert = ownedAlert(alertId);
    alert.markAttended();
    return alert.snapshot();
  }

  /** Closes an owned alert without removing its original evidence. */
  public AlertSummary closeAlert(Long alertId, Instant now) {
    AlertSummary alert = ownedAlert(alertId);
    alert.close(now);
    return alert.snapshot();
  }

  /** Adds an intervention written by this relationship's caregiver. */
  public CaregiverNote addNote(String text, String familiarId, Instant now) {
    if (!this.familiarId.equals(familiarId))
      throw new IllegalArgumentException("note author must be the relationship caregiver");
    CaregiverNote note = new CaregiverNote(text, familiarId, now);
    notes.add(note);
    return note;
  }

  /** Finds an alert by its positive stored identity within this relationship. */
  public AlertSummary findAlert(Long alertId) {
    return ownedAlert(alertId).snapshot();
  }

  private AlertSummary ownedAlert(Long alertId) {
    if (alertId == null || alertId <= 0)
      throw new IllegalArgumentException("alert identity must be positive");
    return alerts.stream()
        .filter(alert -> alertId.equals(alert.getId()))
        .findFirst()
        .orElseThrow(() -> new AlertNotFoundException(alertId));
  }

  /** Returns alerts awaiting explicit closure, including those already attended. */
  public List<AlertSummary> pendingAlerts() {
    return alerts.stream().filter(AlertSummary::isPending).map(AlertSummary::snapshot).toList();
  }

  /** Reports whether any alert still awaits explicit closure. */
  public boolean hasPendingAlerts() {
    return alerts.stream().anyMatch(AlertSummary::isPending);
  }

  /**
   * Marks the medication as low on stock. A second notice for the same medication only refreshes
   * it.
   */
  public LowStockNotice registerLowStock(
      String medicationId,
      String medicationName,
      int remainingStock,
      int replenishmentThreshold,
      Instant now) {
    var existing =
        lowStockNotices.stream()
            .filter(notice -> notice.getMedicationId().equals(medicationId))
            .findFirst();
    if (existing.isPresent()) {
      existing.get().refresh(medicationName, remainingStock, replenishmentThreshold, now);
      return existing.get().snapshot();
    }
    var notice =
        new LowStockNotice(
            medicationId, medicationName, remainingStock, replenishmentThreshold, now);
    lowStockNotices.add(notice);
    return notice.snapshot();
  }

  /**
   * Applies replenished quantity to an existing shortage, retaining its original detection instant.
   * The notice remains pending at the threshold and is removed only when stock exceeds it.
   */
  public void resolveLowStock(String medicationId, int remainingStock) {
    requireText(medicationId, "medicationId");
    if (remainingStock < 0)
      throw new IllegalArgumentException("remaining stock must not be negative");
    var existing =
        lowStockNotices.stream()
            .filter(notice -> notice.getMedicationId().equals(medicationId))
            .findFirst();
    if (existing.isEmpty()) return;
    var notice = existing.get();
    if (remainingStock > notice.getReplenishmentThreshold()) {
      lowStockNotices.remove(notice);
    } else {
      notice.refresh(
          notice.getMedicationName(),
          remainingStock,
          notice.getReplenishmentThreshold(),
          notice.getDetectedAt());
    }
  }

  /** Applies complete stock evidence and retains its source revision after shortage resolution. */
  public boolean projectInventoryStock(InventoryStockPosition incoming, String medicationName) {
    Objects.requireNonNull(incoming, "incoming");
    requireText(medicationName, "medicationName");
    var previous =
        inventoryStockPositions.stream()
            .filter(position -> position.medicationId().equals(incoming.medicationId()))
            .findFirst();
    if (previous.isPresent() && !incoming.advances(previous.get())) return false;
    var existing =
        lowStockNotices.stream()
            .filter(notice -> notice.getMedicationId().equals(incoming.medicationId()))
            .findFirst();
    if (incoming.isLowStock()) {
      var detectedAt = existing.map(LowStockNotice::getDetectedAt).orElse(incoming.changedAt());
      registerLowStock(
          incoming.medicationId(),
          medicationName,
          incoming.remainingStock(),
          incoming.replenishmentThreshold(),
          detectedAt);
    } else {
      existing.ifPresent(lowStockNotices::remove);
    }
    previous.ifPresent(inventoryStockPositions::remove);
    inventoryStockPositions.add(incoming);
    return true;
  }

  /** Returns immutable source positions, including medications whose shortage has cleared. */
  public List<InventoryStockPosition> getInventoryStockPositions() {
    return List.copyOf(inventoryStockPositions);
  }

  /** Keeps the insight once; the same pattern reported again changes nothing. */
  public AdherenceInsight registerInsight(
      String medicationId,
      String medicationName,
      int omissionDays,
      LocalDate firstDay,
      LocalDate lastDay,
      Instant now) {
    return adherenceInsights.stream()
        .filter(insight -> insight.isSamePattern(medicationId, firstDay, lastDay))
        .findFirst()
        .orElseGet(
            () -> {
              var insight =
                  new AdherenceInsight(
                      medicationId, medicationName, omissionDays, firstDay, lastDay, now);
              adherenceInsights.add(insight);
              return insight;
            });
  }

  /** Reports whether at least one low-stock notice remains unresolved. */
  public boolean hasLowStock() {
    return !lowStockNotices.isEmpty();
  }

  /** Most recent first. */
  public List<AdherenceInsight> recentInsights(int limit) {
    if (limit < 1) throw new IllegalArgumentException("insight limit must be positive");
    return adherenceInsights.stream()
        .sorted(Comparator.comparing(AdherenceInsight::getDetectedAt).reversed())
        .limit(limit)
        .toList();
  }

  /**
   * Returns the newest note by recording time, then persisted identity; storage order is
   * irrelevant.
   */
  public CaregiverNote latestNote() {
    return notes.stream()
        .max(
            Comparator.comparing(CaregiverNote::getRecordedAt)
                .thenComparing(
                    CaregiverNote::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElseThrow();
  }

  /** Returns the observed storage revision, absent before the aggregate is first saved. */
  public Long getRevision() {
    return revision;
  }

  /** Returns the stored aggregate identity, absent before first persistence. */
  public Long getId() {
    return id;
  }

  /** Returns the exact relationship owning this follow-up history. */
  public String getCareLinkId() {
    return careLinkId;
  }

  /** Returns the older adult referenced by the relationship. */
  public String getOlderAdultId() {
    return olderAdultId;
  }

  /** Returns the caregiver owning the interventions in this follow-up. */
  public String getFamiliarId() {
    return familiarId;
  }

  /** Returns detached alert snapshots in an immutable list. */
  public List<AlertSummary> getAlerts() {
    return alerts.stream().map(AlertSummary::snapshot).toList();
  }

  /** Returns an unmodifiable view of recorded caregiver interventions. */
  public List<CaregiverNote> getNotes() {
    return Collections.unmodifiableList(notes);
  }

  /** Returns detached unresolved stock snapshots in an immutable list. */
  public List<LowStockNotice> getLowStockNotices() {
    return lowStockNotices.stream().map(LowStockNotice::snapshot).toList();
  }

  /** Restores validated identity, history and audit evidence without replaying transitions. */
  public static FamilyMonitor rehydrate(
      Long id,
      String careLinkId,
      String olderAdultId,
      String familiarId,
      List<AlertSummary> alerts,
      List<CaregiverNote> notes,
      List<LowStockNotice> lowStockNotices,
      List<AdherenceInsight> adherenceInsights,
      Instant createdAt,
      Instant updatedAt,
      Long revision) {
    return rehydrate(
        id,
        careLinkId,
        olderAdultId,
        familiarId,
        alerts,
        notes,
        lowStockNotices,
        adherenceInsights,
        createdAt,
        updatedAt,
        revision,
        List.of());
  }

  /** Restores follow-up together with durable positions for all previously observed inventories. */
  public static FamilyMonitor rehydrate(
      Long id,
      String careLinkId,
      String olderAdultId,
      String familiarId,
      List<AlertSummary> alerts,
      List<CaregiverNote> notes,
      List<LowStockNotice> lowStockNotices,
      List<AdherenceInsight> adherenceInsights,
      Instant createdAt,
      Instant updatedAt,
      Long revision,
      List<InventoryStockPosition> inventoryStockPositions) {
    var positions = List.copyOf(inventoryStockPositions);
    requireDistinct(positions, InventoryStockPosition::medicationId);
    var restored = new FamilyMonitor(careLinkId, olderAdultId, familiarId);
    if (id == null || id <= 0)
      throw new IllegalArgumentException("stored monitor identity must be positive");
    if (revision == null || revision < 0)
      throw new IllegalArgumentException("stored monitor revision is required");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    if (updatedAt.isBefore(createdAt))
      throw new IllegalArgumentException("monitor audit update must not precede creation");
    var storedAlerts = storedChildren(alerts, AlertSummary::getId);
    var storedNotes = storedChildren(notes, CaregiverNote::getId);
    var storedStock = storedChildren(lowStockNotices, LowStockNotice::getId);
    var storedInsights = storedChildren(adherenceInsights, AdherenceInsight::getId);
    requireDistinct(storedAlerts, AlertSummary::getIntakeId);
    requireDistinct(storedStock, LowStockNotice::getMedicationId);
    requireDistinct(
        storedInsights,
        insight -> List.of(insight.getMedicationId(), insight.getFirstDay(), insight.getLastDay()));
    if (storedNotes.stream().anyMatch(note -> !restored.familiarId.equals(note.getFamiliarId())))
      throw new IllegalArgumentException("stored note author differs from relationship caregiver");
    restored.inventoryStockPositions.addAll(positions);
    restored.id = id;
    restored.revision = revision;
    restored.alerts.addAll(storedAlerts.stream().map(AlertSummary::snapshot).toList());
    restored.notes.addAll(storedNotes);
    restored.lowStockNotices.addAll(storedStock.stream().map(LowStockNotice::snapshot).toList());
    restored.adherenceInsights.addAll(storedInsights);
    restored.createdAt = createdAt;
    restored.updatedAt = updatedAt;
    return restored;
  }

  private static <T> List<T> storedChildren(List<T> children, Function<T, Long> identity) {
    var copy = List.copyOf(children);
    var identities = new HashSet<Long>();
    for (T child : copy) {
      Long id = identity.apply(child);
      if (id == null || id <= 0 || !identities.add(id))
        throw new IllegalArgumentException("stored child identities must be positive and distinct");
    }
    return copy;
  }

  private static <T> void requireDistinct(List<T> children, Function<T, ?> key) {
    var keys = new HashSet<>();
    for (T child : children)
      if (!keys.add(key.apply(child)))
        throw new IllegalArgumentException("stored follow-up contains duplicate source evidence");
  }

  /** Returns an unmodifiable view of stored source analytics patterns. */
  public List<AdherenceInsight> getAdherenceInsights() {
    return Collections.unmodifiableList(adherenceInsights);
  }

  /** Returns the first persistence audit instant, absent before saving. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the latest persistence audit instant, absent before saving. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
