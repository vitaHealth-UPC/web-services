package com.vitahealth.tata.omissionescalation.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.omissionescalation.domain.model.entities.CareAlert;
import com.vitahealth.tata.omissionescalation.domain.model.entities.EscalationRecord;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.AlertStatus;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.EscalationLevel;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Life cycle of an unconfirmed intake: grace period, omission, caregiver alert and escalation. The
 * intake and the older adult are referenced by logical id only.
 */
public class OmissionCase {

  private Long id;

  private String intakeId;

  private String olderAdultId;

  private String medicationName;

  private Instant scheduledAt;

  private OmissionCaseStatus status;

  private GracePeriod gracePeriod;

  private Instant reinforcedReminderSentAt;

  private Instant omittedAt;

  private Instant closedAt;

  private List<CareAlert> alerts = new ArrayList<>();

  private List<EscalationRecord> escalations = new ArrayList<>();

  private Instant createdAt;

  private Instant updatedAt;

  protected OmissionCase() {}

  /**
   * Opens a pending case with required source references and an explicit clinical grace interval.
   */
  public OmissionCase(
      String intakeId,
      String olderAdultId,
      String medicationName,
      Instant scheduledAt,
      GracePeriod gracePeriod) {
    if (scheduledAt == null || gracePeriod == null) {
      throw new IllegalArgumentException("scheduled time and grace period are required");
    }
    this.intakeId = requireText(intakeId, "intakeId");
    this.olderAdultId = requireText(olderAdultId, "olderAdultId");
    this.medicationName = requireText(medicationName, "medicationName");
    this.scheduledAt = scheduledAt;
    this.gracePeriod = gracePeriod;
    this.status = OmissionCaseStatus.PENDING;
  }

  /** Checks the inclusive end of the clinical grace interval at the supplied evaluation time. */
  public boolean isGraceExpired(Instant now) {
    return gracePeriod.isExpired(now);
  }

  /** Records the first accepted reminder without replacing its original delivery time. */
  public void markReminderSent(Instant now) {
    requireStatus(OmissionCaseStatus.PENDING);
    if (now == null) throw new IllegalArgumentException("reminder delivery time is required");
    if (reinforcedReminderSentAt == null) {
      this.reinforcedReminderSentAt = now;
    }
  }

  /**
   * Closes a pending case after Intake Execution records a confirmation. A definitive omission
   * remains unchanged.
   */
  public void resolve() {
    requireStatus(OmissionCaseStatus.PENDING);
    this.status = OmissionCaseStatus.RESOLVED;
  }

  /** The grace period ended without confirmation. */
  public void markOmitted(Instant now) {
    requireStatus(OmissionCaseStatus.PENDING);
    if (!gracePeriod.isExpired(now)) {
      throw new IllegalStateException("The grace period has not expired yet");
    }
    this.status = OmissionCaseStatus.OMITTED;
    this.omittedAt = now;
  }

  /** Creates the caregiver alert once; repeated calls return the existing one. */
  public CareAlert addAlert(Instant now) {
    requireStatus(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED);
    if (!alerts.isEmpty()) {
      return alerts.getFirst();
    }
    if (now == null || now.isBefore(omittedAt)) {
      throw new IllegalArgumentException("alert time must not precede omission");
    }
    CareAlert alert = new CareAlert(now);
    alerts.add(alert);
    return alert;
  }

  /** Records successful delivery on an active case while retaining the first accepted evidence. */
  public void markAlertSent(Instant acceptedAt) {
    CareAlert alert = requireActiveAlert();
    if (acceptedAt == null || acceptedAt.isBefore(alert.getGeneratedAt()))
      throw new IllegalArgumentException("send time must not precede generation");
    if (alert.getStatus() != AlertStatus.SENT)
      alerts.set(0, alert.withSuccessfulDelivery(acceptedAt));
  }

  /** Records failed business delivery evidence without replacing an already accepted alert. */
  public void markAlertFailed(String reason) {
    alerts.set(0, requireActiveAlert().withFailedDelivery(reason));
  }

  private CareAlert requireActiveAlert() {
    requireStatus(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED);
    if (alerts.size() != 1) throw new IllegalStateException("business alert is missing");
    return alerts.getFirst();
  }

  /** Appends the next attention level without allowing occurrence times to move backwards. */
  public EscalationRecord escalate(String reason, Instant now) {
    requireStatus(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED);
    if (now == null || lastActivityAt() == null || now.isBefore(lastActivityAt()))
      throw new IllegalArgumentException("escalation time must not precede the latest activity");
    EscalationLevel nextLevel = currentLevel().next();
    EscalationRecord record = new EscalationRecord(nextLevel, reason, now);
    escalations.add(record);
    this.status = OmissionCaseStatus.ESCALATED;
    return record;
  }

  /** Closes an active omission at or after its latest recorded activity. */
  public void close(Instant now) {
    requireStatus(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED);
    if (now == null || lastActivityAt() == null || now.isBefore(lastActivityAt()))
      throw new IllegalArgumentException("closure time must not precede the latest activity");
    this.status = OmissionCaseStatus.CLOSED;
    this.closedAt = now;
  }

  /** Returns the latest recorded level, or zero before the first escalation. */
  public EscalationLevel currentLevel() {
    if (escalations.isEmpty()) {
      return EscalationLevel.NONE;
    }
    return escalations.getLast().getLevel();
  }

  /** Last moment something happened in the omitted flow: the last escalation or the omission. */
  public Instant lastActivityAt() {
    if (escalations.isEmpty()) {
      return omittedAt;
    }
    return escalations.getLast().getTriggeredAt();
  }

  private void requireStatus(OmissionCaseStatus... allowed) {
    for (OmissionCaseStatus candidate : allowed) {
      if (status == candidate) {
        return;
      }
    }
    throw new IllegalStateException("Operation not allowed while the case is " + status);
  }

  /** Returns the stored case identity, absent before its first save. */
  public Long getId() {
    return id;
  }

  /** Returns the original scheduled intake reference. */
  public String getIntakeId() {
    return intakeId;
  }

  /** Returns the adult reference captured when the case opened. */
  public String getOlderAdultId() {
    return olderAdultId;
  }

  /** Returns the medication name captured with the scheduled intake. */
  public String getMedicationName() {
    return medicationName;
  }

  /** Returns the original intake schedule time. */
  public Instant getScheduledAt() {
    return scheduledAt;
  }

  /** Returns the current clinical lifecycle state. */
  public OmissionCaseStatus getStatus() {
    return status;
  }

  /** Returns the clinical interval used to decide definitive omission. */
  public GracePeriod getGracePeriod() {
    return gracePeriod;
  }

  /** Returns the first recorded reminder acceptance time, or null while unsent. */
  public Instant getReinforcedReminderSentAt() {
    return reinforcedReminderSentAt;
  }

  /** Returns the definitive omission time, or null before omission. */
  public Instant getOmittedAt() {
    return omittedAt;
  }

  /** Returns the clinical closure time, or null unless the case is closed. */
  public Instant getClosedAt() {
    return closedAt;
  }

  /** Returns immutable alert snapshots through a structurally unmodifiable collection view. */
  public List<CareAlert> getAlerts() {
    return Collections.unmodifiableList(alerts);
  }

  /** Returns the ordered history through a collection view that prevents structural changes. */
  public List<EscalationRecord> getEscalations() {
    return Collections.unmodifiableList(escalations);
  }

  /** Restores persisted state without replaying business actions. */
  public static OmissionCase rehydrate(
      Long id,
      String intakeId,
      String olderAdultId,
      String medicationName,
      Instant scheduledAt,
      OmissionCaseStatus status,
      GracePeriod gracePeriod,
      Instant reinforcedReminderSentAt,
      Instant omittedAt,
      Instant closedAt,
      List<CareAlert> alerts,
      List<EscalationRecord> escalations,
      Instant createdAt,
      Instant updatedAt) {
    var restored = new OmissionCase();
    restored.id = id;
    restored.intakeId = intakeId;
    restored.olderAdultId = olderAdultId;
    restored.medicationName = medicationName;
    restored.scheduledAt = scheduledAt;
    restored.status = status;
    restored.gracePeriod = gracePeriod;
    restored.reinforcedReminderSentAt = reinforcedReminderSentAt;
    restored.omittedAt = omittedAt;
    restored.closedAt = closedAt;
    restored.alerts = new ArrayList<>(alerts);
    restored.escalations = validatedHistory(escalations, omittedAt);
    restored.createdAt = createdAt;
    restored.updatedAt = updatedAt;
    restored.validateStoredState();
    return restored;
  }

  private void validateStoredState() {
    if (id == null
        || id < 1
        || intakeId == null
        || intakeId.isBlank()
        || olderAdultId == null
        || olderAdultId.isBlank()
        || medicationName == null
        || medicationName.isBlank()
        || scheduledAt == null
        || status == null
        || gracePeriod == null
        || alerts.size() > 1
        || alerts.stream().anyMatch(alert -> alert == null))
      throw new IllegalArgumentException("stored omission references and evidence are required");
    boolean omitted =
        status == OmissionCaseStatus.OMITTED
            || status == OmissionCaseStatus.ESCALATED
            || status == OmissionCaseStatus.CLOSED;
    if (omitted) {
      if (omittedAt == null || omittedAt.isBefore(gracePeriod.endsAt()))
        throw new IllegalArgumentException("stored omission time is inconsistent");
      if (alerts.stream().anyMatch(alert -> alert.getGeneratedAt().isBefore(omittedAt)))
        throw new IllegalArgumentException("stored alert precedes omission");
    } else if (omittedAt != null || !alerts.isEmpty() || !escalations.isEmpty()) {
      throw new IllegalArgumentException("unomitted case contains omission evidence");
    }
    if (status == OmissionCaseStatus.OMITTED && !escalations.isEmpty()
        || status == OmissionCaseStatus.ESCALATED && escalations.isEmpty())
      throw new IllegalArgumentException("stored status contradicts escalation history");
    if (status == OmissionCaseStatus.CLOSED) {
      if (closedAt == null || closedAt.isBefore(lastActivityAt()))
        throw new IllegalArgumentException("stored closure time is inconsistent");
    } else if (closedAt != null) {
      throw new IllegalArgumentException("active case contains closure evidence");
    }
  }

  private static List<EscalationRecord> validatedHistory(
      List<EscalationRecord> records, Instant omittedAt) {
    if (records == null || records.stream().anyMatch(record -> record == null))
      throw new IllegalArgumentException("escalation history is required");
    var ordered = new ArrayList<>(records);
    ordered.sort(Comparator.comparingInt(record -> record.getLevel().value()));
    var previous = omittedAt;
    for (int index = 0; index < ordered.size(); index++) {
      var record = ordered.get(index);
      if (record.getLevel().value() != index + 1
          || previous == null
          || record.getTriggeredAt().isBefore(previous))
        throw new IllegalArgumentException("stored escalation history is inconsistent");
      previous = record.getTriggeredAt();
    }
    return ordered;
  }

  /** Returns storage creation evidence, absent before the first save. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the most recent storage update time. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
