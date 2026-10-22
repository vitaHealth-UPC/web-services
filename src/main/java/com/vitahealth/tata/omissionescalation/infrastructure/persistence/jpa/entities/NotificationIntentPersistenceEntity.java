package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationIntentState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Source-case intention with a recoverable fanout reservation and committed scan cursors. */
@Entity
@Table(
    name = "omission_notification_intents",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_omission_notification_source_kind",
            columnNames = {"omission_case_id", "kind"}),
    indexes =
        @Index(
            name = "idx_omission_notification_fanout_due",
            columnList = "state,next_attempt_at,id"))
public class NotificationIntentPersistenceEntity {
  @Id
  @Column(length = 36)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "omission_case_id", nullable = false)
  private OmissionCasePersistenceEntity sourceCase;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private NotificationKind kind;

  @Column(nullable = false)
  private Instant requestedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private NotificationIntentState state;

  @Column(nullable = false)
  private Instant nextAttemptAt;

  @Column(length = 36)
  private String leaseToken;

  private Instant leaseUntil;

  private Instant leaseStartedAt;

  @Column(nullable = false)
  private String caregiverAfterId;

  private String currentCaregiverId;

  @Column(nullable = false, length = 36)
  private String deviceAfterId;

  /** Constructor required by JPA hydration. */
  protected NotificationIntentPersistenceEntity() {}

  /** Creates ready work while retaining its existing clinical source by reference. */
  public NotificationIntentPersistenceEntity(
      OmissionCasePersistenceEntity sourceCase, NotificationKind kind, Instant requestedAt) {
    this.sourceCase = Objects.requireNonNull(sourceCase, "sourceCase");
    if (sourceCase.getId() == null || sourceCase.getId() < 1)
      throw new IllegalArgumentException("notification source must be persisted");
    this.kind = Objects.requireNonNull(kind, "kind");
    this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    this.id = UUID.randomUUID().toString();
    this.state = NotificationIntentState.READY;
    this.nextAttemptAt = requestedAt;
    this.caregiverAfterId = "";
    this.deviceAfterId = "";
  }

  /** Claims due work without allowing a second worker to replace an unexpired reservation. */
  public boolean claimFanout(String token, Instant now, Duration lease) {
    requireConsistentState();
    if (token == null
        || token.isBlank()
        || token.length() > 36
        || now == null
        || lease == null
        || lease.isNegative()
        || lease.isZero()) throw new IllegalArgumentException("fanout reservation is invalid");
    if (state != NotificationIntentState.READY
        || nextAttemptAt.isAfter(now)
        || leaseUntil != null && leaseUntil.isAfter(now)) return false;
    var until = now.plus(lease);
    leaseToken = token;
    leaseStartedAt = now;
    leaseUntil = until;
    return true;
  }

  /** Requires both current worker identity and a still-live reservation. */
  public boolean ownsFanout(String token, Instant now) {
    requireConsistentState();
    return state == NotificationIntentState.READY
        && token != null
        && token.equals(leaseToken)
        && now != null
        && leaseStartedAt != null
        && !now.isBefore(leaseStartedAt)
        && leaseUntil != null
        && leaseUntil.isAfter(now);
  }

  /** Stores only progress owned by the current lease, before a page transaction commits. */
  public boolean advanceFanout(
      String token,
      Instant now,
      String caregiverAfterId,
      String currentCaregiverId,
      String deviceAfterId) {
    if (!ownsFanout(token, now)) return false;
    if (caregiverAfterId == null
        || caregiverAfterId.length() > 255
        || currentCaregiverId != null
            && (currentCaregiverId.isBlank() || currentCaregiverId.length() > 255)
        || deviceAfterId == null
        || deviceAfterId.length() > 36)
      throw new IllegalArgumentException("fanout cursor is invalid");
    this.caregiverAfterId = caregiverAfterId;
    this.currentCaregiverId = currentCaregiverId;
    this.deviceAfterId = deviceAfterId;
    return true;
  }

  /** Releases a live lease for another bounded page or a delayed retry without losing cursors. */
  public boolean rescheduleFanout(String token, Instant now, Duration delay) {
    if (!ownsFanout(token, now)) return false;
    if (delay == null || delay.isNegative())
      throw new IllegalArgumentException("fanout delay is invalid");
    nextAttemptAt = now.plus(delay);
    clearLease();
    return true;
  }

  /** Records fanout completion only; device acceptance and aggregate sent evidence are separate. */
  public boolean completeFanout(String token, Instant now) {
    if (!ownsFanout(token, now)) return false;
    state = NotificationIntentState.FANOUT_COMPLETE;
    clearLease();
    return true;
  }

  /** Records source-case completion only after the caller verifies all durable device outcomes. */
  public boolean completeDelivery() {
    requireConsistentState();
    if (state != NotificationIntentState.FANOUT_COMPLETE) return false;
    state = NotificationIntentState.COMPLETED;
    return true;
  }

  /** Stops unfinished technical work without fabricating successful delivery. */
  public void cancel() {
    state = NotificationIntentState.CANCELLED;
    clearLease();
  }

  private void requireConsistentState() {
    if (id == null
        || sourceCase == null
        || kind == null
        || requestedAt == null
        || state == null
        || nextAttemptAt == null
        || nextAttemptAt.isBefore(requestedAt)
        || caregiverAfterId == null
        || deviceAfterId == null
        || (leaseToken == null) != (leaseUntil == null)
        || (leaseToken == null) != (leaseStartedAt == null)
        || leaseToken != null
            && (leaseToken.isBlank()
                || leaseToken.length() > 36
                || leaseStartedAt.isBefore(requestedAt)
                || !leaseUntil.isAfter(leaseStartedAt))
        || state != NotificationIntentState.READY && leaseToken != null)
      throw new IllegalStateException("stored notification intention is inconsistent");
  }

  private void clearLease() {
    leaseToken = null;
    leaseStartedAt = null;
    leaseUntil = null;
  }

  /** Returns stable intention identity. */
  public String getId() {
    return id;
  }

  /** Returns the owning clinical source identity without copying medication data. */
  public long getOmissionCaseId() {
    return sourceCase.getId();
  }

  /** Returns reminder versus caregiver alert routing. */
  public NotificationKind getKind() {
    return kind;
  }

  /** Returns original request time retained across retries. */
  public Instant getRequestedAt() {
    return requestedAt;
  }

  /** Returns technical fanout state, not clinical delivery success. */
  public NotificationIntentState getState() {
    return state;
  }

  /** Returns the committed caregiver scan position. */
  public String getCaregiverAfterId() {
    return caregiverAfterId;
  }

  /** Returns the caregiver whose device pages are currently being scanned. */
  public String getCurrentCaregiverId() {
    return currentCaregiverId;
  }

  /** Returns the committed installation scan position. */
  public String getDeviceAfterId() {
    return deviceAfterId;
  }

  /** Returns reservation expiry for recovery diagnostics without exposing its token. */
  public Instant getLeaseUntil() {
    return leaseUntil;
  }
}
