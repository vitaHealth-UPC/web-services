package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities;

import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationDeliveryState;
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

/**
 * Durable per-installation attempt evidence; device addresses are resolved only at dispatch time.
 */
@Entity
@Table(
    name = "omission_device_deliveries",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_omission_delivery_intent_installation",
            columnNames = {"intent_id", "installation_id"}),
    indexes =
        @Index(name = "idx_omission_device_delivery_due", columnList = "state,next_attempt_at,id"))
public class NotificationDeviceDeliveryPersistenceEntity {
  @Id
  @Column(length = 36)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "intent_id", nullable = false)
  private NotificationIntentPersistenceEntity intent;

  @Column(name = "installation_id", nullable = false, length = 36)
  private String installationId;

  @Column(nullable = false)
  private String sessionId;

  @Column(nullable = false)
  private String recipientId;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant nextAttemptAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private NotificationDeliveryState state;

  @Column(nullable = false)
  private int attempts;

  @Column(length = 36)
  private String leaseToken;

  private Instant leaseStartedAt;
  private Instant leaseUntil;

  @Column(length = 512)
  private String providerReceipt;

  private Instant acceptedAt;

  /** Constructor required for JPA hydration. */
  protected NotificationDeviceDeliveryPersistenceEntity() {}

  /**
   * Retains original session ownership so account switches cannot redirect an existing delivery.
   */
  public NotificationDeviceDeliveryPersistenceEntity(
      NotificationIntentPersistenceEntity intent,
      String installationId,
      String sessionId,
      String recipientId,
      Instant now) {
    this.intent = Objects.requireNonNull(intent, "intent");
    if (installationId == null
        || !UUID.fromString(installationId).toString().equals(installationId))
      throw new IllegalArgumentException("installation id is invalid");
    if (sessionId == null
        || sessionId.isBlank()
        || sessionId.length() > 255
        || recipientId == null
        || recipientId.isBlank()
        || recipientId.length() > 255)
      throw new IllegalArgumentException("delivery ownership is required");
    this.createdAt = Objects.requireNonNull(now, "now");
    if (now.isBefore(intent.getRequestedAt()))
      throw new IllegalArgumentException("delivery precedes intention");
    this.id = UUID.randomUUID().toString();
    this.installationId = installationId;
    this.sessionId = sessionId;
    this.recipientId = recipientId;
    this.nextAttemptAt = now;
    this.state = NotificationDeliveryState.READY;
  }

  /** Reserves a due address without taking over a live worker lease. */
  public boolean claim(String worker, Instant now, Duration duration) {
    requireConsistentState();
    if (worker == null
        || worker.isBlank()
        || worker.length() > 36
        || now == null
        || duration == null
        || duration.isNegative()
        || duration.isZero()) throw new IllegalArgumentException("delivery reservation is invalid");
    if (state != NotificationDeliveryState.READY
        || nextAttemptAt.isAfter(now)
        || leaseUntil != null && leaseUntil.isAfter(now)) return false;
    var until = now.plus(duration);
    leaseToken = worker;
    leaseStartedAt = now;
    leaseUntil = until;
    return true;
  }

  /** Checks worker identity and reservation chronology before any completion or retry. */
  public boolean owns(String worker, Instant now) {
    requireConsistentState();
    return state == NotificationDeliveryState.READY
        && worker != null
        && worker.equals(leaseToken)
        && now != null
        && !now.isBefore(leaseStartedAt)
        && leaseUntil.isAfter(now);
  }

  /**
   * Records provider acceptance once; expired workers cannot replace a newer attempt's evidence.
   */
  public boolean accept(String worker, Instant now, String receipt) {
    if (!owns(worker, now)) return false;
    if (receipt == null || receipt.isBlank() || receipt.length() > 512)
      throw new IllegalArgumentException("provider acceptance receipt is invalid");
    providerReceipt = receipt;
    acceptedAt = now;
    state = NotificationDeliveryState.ACCEPTED;
    clearLease();
    return true;
  }

  /**
   * Retains original ownership and schedules a bounded exponential retry without storing error
   * payloads.
   */
  public boolean retry(String worker, Instant now) {
    if (!owns(worker, now)) return false;
    int nextAttempts =
        Math.min(Integer.MAX_VALUE, attempts == Integer.MAX_VALUE ? attempts : attempts + 1);
    var next = now.plusSeconds(Math.min(300, 5L << Math.min(nextAttempts, 6)));
    attempts = nextAttempts;
    nextAttemptAt = next;
    clearLease();
    return true;
  }

  /** Cancels unsent work only; successful provider evidence remains immutable. */
  public void cancel() {
    requireConsistentState();
    if (state == NotificationDeliveryState.ACCEPTED) return;
    state = NotificationDeliveryState.CANCELLED;
    clearLease();
  }

  private void clearLease() {
    leaseToken = null;
    leaseStartedAt = null;
    leaseUntil = null;
  }

  private void requireConsistentState() {
    boolean leased = leaseToken != null;
    if (state == null
        || createdAt == null
        || nextAttemptAt == null
        || nextAttemptAt.isBefore(createdAt)
        || attempts < 0
        || leased != (leaseStartedAt != null)
        || leased != (leaseUntil != null)
        || leased
            && (leaseToken.isBlank()
                || !leaseUntil.isAfter(leaseStartedAt)
                || leaseStartedAt.isBefore(createdAt)
                || state != NotificationDeliveryState.READY)
        || (state == NotificationDeliveryState.ACCEPTED)
            != (providerReceipt != null && !providerReceipt.isBlank() && acceptedAt != null)
        || state != NotificationDeliveryState.ACCEPTED
            && (providerReceipt != null || acceptedAt != null)
        || acceptedAt != null && acceptedAt.isBefore(createdAt))
      throw new IllegalStateException("stored device delivery is inconsistent");
  }

  /** Returns stable delivery identity for device-side duplicate suppression. */
  public String getId() {
    return id;
  }

  /** Returns its durable business intention reference. */
  public String getIntentId() {
    return intent.getId();
  }

  /** Returns the selected installation identity, never its current opaque token. */
  public String getInstallationId() {
    return installationId;
  }

  /** Returns the original revocable session reference required at dispatch. */
  public String getSessionId() {
    return sessionId;
  }

  /** Returns the logical caregiver or adult whose authorization must be rechecked. */
  public String getRecipientId() {
    return recipientId;
  }

  /** Returns the technical provider outcome. */
  public NotificationDeliveryState getState() {
    return state;
  }

  /** Returns the original selection time. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns bounded retry eligibility time. */
  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  /** Returns recorded failed attempts. */
  public int getAttempts() {
    return attempts;
  }

  /** Returns provider acceptance evidence, absent before acceptance. */
  public String getProviderReceipt() {
    return providerReceipt;
  }

  /** Returns acceptance time, not handset display or clinical confirmation time. */
  public Instant getAcceptedAt() {
    return acceptedAt;
  }
}
