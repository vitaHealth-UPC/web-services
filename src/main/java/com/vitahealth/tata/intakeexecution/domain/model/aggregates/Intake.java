package com.vitahealth.tata.intakeexecution.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Owns a scheduled intake and its definitive confirmation or omission evidence. */
public final class Intake {
  private final String id;
  private final String treatmentId;
  private String medicationId;
  private final String olderAdultId;
  private MedicationSnapshot medication;
  private final Instant scheduledAt;
  private IntakeStatus status;
  private final Instant createdAt;
  private Instant confirmedAt;
  private ConfirmationChannel confirmationChannel;
  private Instant unconfirmedReportedAt;

  private Intake(
      String id,
      String treatmentId,
      String medicationId,
      String olderAdultId,
      MedicationSnapshot medication,
      Instant scheduledAt,
      IntakeStatus status,
      Instant createdAt) {
    this.id = requireText(id, "id");
    this.treatmentId = requireText(treatmentId, "treatmentId");
    this.medicationId = requireText(medicationId, "medicationId");
    this.olderAdultId = requireText(olderAdultId, "olderAdultId");
    this.medication = Objects.requireNonNull(medication);
    this.scheduledAt = Objects.requireNonNull(scheduledAt);
    this.status = Objects.requireNonNull(status);
    this.createdAt = Objects.requireNonNull(createdAt);
  }

  /** Creates a pending slot using the supplied application instant without inventing a clock. */
  public static Intake createScheduled(
      String treatmentId,
      String medicationId,
      String olderAdultId,
      MedicationSnapshot medication,
      Instant scheduledAt,
      Instant now) {
    return new Intake(
        UUID.randomUUID().toString(),
        treatmentId,
        medicationId,
        olderAdultId,
        medication,
        scheduledAt,
        IntakeStatus.PENDING,
        now);
  }

  /** Restores the supplied persisted identity and evidence without generating timestamps. */
  public static Intake rehydrate(
      String id,
      String treatmentId,
      String medicationId,
      String olderAdultId,
      MedicationSnapshot medication,
      Instant scheduledAt,
      IntakeStatus status,
      Instant createdAt) {
    return rehydrate(
        id,
        treatmentId,
        medicationId,
        olderAdultId,
        medication,
        scheduledAt,
        status,
        createdAt,
        null,
        null,
        null);
  }

  /** Restores the supplied persisted identity and evidence without generating timestamps. */
  public static Intake rehydrate(
      String id,
      String treatmentId,
      String medicationId,
      String olderAdultId,
      MedicationSnapshot medication,
      Instant scheduledAt,
      IntakeStatus status,
      Instant createdAt,
      Instant confirmedAt,
      ConfirmationChannel channel) {
    return rehydrate(
        id,
        treatmentId,
        medicationId,
        olderAdultId,
        medication,
        scheduledAt,
        status,
        createdAt,
        confirmedAt,
        channel,
        null);
  }

  /** Restores the supplied persisted identity and evidence without generating timestamps. */
  public static Intake rehydrate(
      String id,
      String treatmentId,
      String medicationId,
      String olderAdultId,
      MedicationSnapshot medication,
      Instant scheduledAt,
      IntakeStatus status,
      Instant createdAt,
      Instant confirmedAt,
      ConfirmationChannel channel,
      Instant unconfirmedReportedAt) {
    boolean confirmed = status == IntakeStatus.CONFIRMED || status == IntakeStatus.LATE;
    if ((confirmed && (confirmedAt == null || channel == null))
        || (!confirmed && (confirmedAt != null || channel != null)))
      throw new IllegalArgumentException(
          "intake confirmation evidence is inconsistent with its status");
    var intake =
        new Intake(
            id,
            treatmentId,
            medicationId,
            olderAdultId,
            medication,
            scheduledAt,
            status,
            createdAt);
    intake.confirmedAt = confirmedAt;
    intake.confirmationChannel = channel;
    intake.unconfirmedReportedAt = unconfirmedReportedAt;
    return intake;
  }

  /**
   * Updates only pending medication details while retaining slot identity and registration
   * evidence.
   */
  public void revisePendingMedication(String medicationId, MedicationSnapshot medication) {
    var validatedId = requireText(medicationId, "medicationId");
    var validatedSnapshot = Objects.requireNonNull(medication, "medication");
    if (!isPending())
      throw new IllegalStateException("definitive intake evidence cannot be revised");
    this.medicationId = validatedId;
    this.medication = validatedSnapshot;
  }

  /** Returns whether the scheduled intake can still receive a definitive outcome. */
  public boolean isPending() {
    return status == IntakeStatus.PENDING;
  }

  /**
   * Confirms a pending intake exactly once.
   *
   * <p>A confirmation after the scheduled instant is classified as LATE while the intake remains
   * pending. CONFIRMED and LATE are definitive confirmation outcomes, so a retry is an idempotent
   * no-op. An OMITTED intake cannot be replaced implicitly by a later confirmation.
   */
  public boolean confirm(Instant now, ConfirmationChannel channel) {
    Objects.requireNonNull(now);
    Objects.requireNonNull(channel);

    if (status == IntakeStatus.CONFIRMED || status == IntakeStatus.LATE) {
      return false;
    }
    if (status != IntakeStatus.PENDING) {
      throw new IllegalStateException("intake can no longer be confirmed");
    }

    status = now.isAfter(scheduledAt) ? IntakeStatus.LATE : IntakeStatus.CONFIRMED;
    confirmedAt = now;
    confirmationChannel = channel;
    return true;
  }

  /**
   * Marks that the unconfirmed outcome was handed off to Omission & Escalation. This
   * technical/business hand-off marker prevents duplicate cross-context events when the scheduler
   * retries.
   */
  public boolean reportUnconfirmed(Instant reportedAt) {
    Objects.requireNonNull(reportedAt);
    if (status != IntakeStatus.PENDING || unconfirmedReportedAt != null) {
      return false;
    }
    unconfirmedReportedAt = reportedAt;
    return true;
  }

  /**
   * Applies the definitive omission outcome emitted by Omission & Escalation. A persisted
   * confirmation is definitive, including a late confirmation. A delayed omission event cannot
   * replace its original time or channel.
   */
  public boolean markOmitted() {
    if (status != IntakeStatus.PENDING) {
      return false;
    }
    status = IntakeStatus.OMITTED;
    return true;
  }

  /** Returns the immutable intake identifier. */
  public String id() {
    return id;
  }

  /** Returns the source treatment identifier. */
  public String treatmentId() {
    return treatmentId;
  }

  /** Returns the medication reference captured for this slot. */
  public String medicationId() {
    return medicationId;
  }

  /** Returns the adult owning the evidence. */
  public String olderAdultId() {
    return olderAdultId;
  }

  /** Returns the medication snapshot retained with the intake. */
  public MedicationSnapshot medication() {
    return medication;
  }

  /** Returns the scheduled instant independent of presentation time zone. */
  public Instant scheduledAt() {
    return scheduledAt;
  }

  /** Returns the current clinical outcome. */
  public IntakeStatus status() {
    return status;
  }

  /** Returns the original slot registration instant. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns the recorded confirmation instant, or null when absent. */
  public Instant confirmedAt() {
    return confirmedAt;
  }

  /** Returns the original confirmation channel, or null when absent. */
  public ConfirmationChannel confirmationChannel() {
    return confirmationChannel;
  }

  /** Returns the committed handoff marker, or null before reporting. */
  public Instant unconfirmedReportedAt() {
    return unconfirmedReportedAt;
  }
}
