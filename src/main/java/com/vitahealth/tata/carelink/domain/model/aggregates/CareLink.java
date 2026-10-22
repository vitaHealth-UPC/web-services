package com.vitahealth.tata.carelink.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.model.valueobjects.Consent;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Owns linking-code acceptance, adult consent and confirmed care relationship state. */
public final class CareLink {
  private final String id;
  private final String caregiverId;
  private final String olderAdultId;
  private CareLinkStatus status;
  private LinkingCode linkingCode;
  private Consent consent;
  private Instant confirmedAt;
  private final Instant createdAt;
  private Instant updatedAt;

  private CareLink(
      String id,
      String caregiverId,
      String olderAdultId,
      CareLinkStatus status,
      LinkingCode linkingCode,
      Consent consent,
      Instant confirmedAt,
      Instant createdAt,
      Instant updatedAt) {
    this.id = requireText(id, "id");
    this.caregiverId = requireText(caregiverId, "caregiverId");
    this.olderAdultId = requireText(olderAdultId, "olderAdultId");
    this.status = Objects.requireNonNull(status);
    this.linkingCode = Objects.requireNonNull(linkingCode);
    this.consent = Objects.requireNonNull(consent);
    this.confirmedAt = confirmedAt;
    this.createdAt = Objects.requireNonNull(createdAt);
    this.updatedAt = Objects.requireNonNull(updatedAt);
    requireConsistentStoredState();
  }

  /** Creates a pending relationship using unused, currently valid linking evidence. */
  public static CareLink createPending(
      String caregiverId, String olderAdultId, LinkingCode linkingCode, Instant now) {
    Objects.requireNonNull(now);
    if (!Objects.requireNonNull(linkingCode, "linkingCode").isValid(now)) {
      throw new IllegalArgumentException("a new care link requires an unused valid code");
    }
    return new CareLink(
        UUID.randomUUID().toString(),
        caregiverId,
        olderAdultId,
        CareLinkStatus.PENDING,
        linkingCode,
        Consent.pending(),
        null,
        now,
        now);
  }

  /** Restores stored relationship state without repeating acceptance or consent transitions. */
  public static CareLink rehydrate(
      String id,
      String caregiverId,
      String olderAdultId,
      CareLinkStatus status,
      LinkingCode linkingCode,
      Consent consent,
      Instant confirmedAt,
      Instant createdAt,
      Instant updatedAt) {
    return new CareLink(
        id,
        caregiverId,
        olderAdultId,
        status,
        linkingCode,
        consent,
        confirmedAt,
        createdAt,
        updatedAt);
  }

  /** Consumes matching linking evidence and starts consent onboarding atomically. */
  public void accept(String suppliedCode, Instant now) {
    if (status != CareLinkStatus.PENDING) {
      throw new IllegalStateException("care link is not pending");
    }
    requireTransitionTime(now);
    if (suppliedCode == null || !linkingCode.value().equalsIgnoreCase(suppliedCode.strip())) {
      throw new IllegalArgumentException("linking code is invalid");
    }
    linkingCode = linkingCode.useAt(now);
    status = CareLinkStatus.AWAITING_CONSENT;
    updatedAt = now;
  }

  /** Records adult consent; a rejection revokes the pending relationship. */
  public void registerConsent(boolean accepted, Instant now) {
    if (status != CareLinkStatus.AWAITING_CONSENT) {
      throw new IllegalStateException("care link is not awaiting consent");
    }
    requireTransitionTime(now);
    consent = Consent.record(accepted, now);
    updatedAt = now;
    if (!accepted) {
      status = CareLinkStatus.REVOKED;
    }
  }

  /** Confirms a consented relationship only after validating the replacement timestamp. */
  public void confirm(Instant now) {
    if (status != CareLinkStatus.AWAITING_CONSENT) {
      throw new IllegalStateException("care link is not awaiting consent");
    }
    if (!consent.isGranted()) {
      throw new IllegalStateException("consent is required");
    }
    requireTransitionTime(now);
    status = CareLinkStatus.CONFIRMED;
    confirmedAt = now;
    updatedAt = now;
  }

  /** Withdraws the linked adult's confirmed decision without erasing confirmation history. */
  public boolean withdrawConsent(String actingOlderAdultId, Instant now) {
    if (!olderAdultId.equals(requireText(actingOlderAdultId, "actingOlderAdultId"))) {
      throw new IllegalArgumentException("only the linked adult can withdraw consent");
    }
    if (status == CareLinkStatus.REVOKED && !consent.isGranted()) return false;
    if (!isActive())
      throw new IllegalStateException("confirmed consent is required for withdrawal");
    requireTransitionTime(now);
    var withdrawal = Consent.record(false, now);
    consent = withdrawal;
    status = CareLinkStatus.REVOKED;
    updatedAt = now;
    return true;
  }

  /** Reports whether confirmed consent currently permits relationship access. */
  public boolean isActive() {
    return status == CareLinkStatus.CONFIRMED && consent.isGranted();
  }

  private void requireTransitionTime(Instant now) {
    Objects.requireNonNull(now, "now");
    if (now.isBefore(updatedAt)) {
      throw new IllegalArgumentException(
          "transition time cannot precede current relationship state");
    }
  }

  /** Checks lifecycle evidence without interpreting expiration against the current clock. */
  private void requireConsistentStoredState() {
    var usedAt = linkingCode.usedAt();
    var decisionAt = consent.recordedAt();
    if (updatedAt.isBefore(createdAt) || !linkingCode.expiresAt().isAfter(createdAt)) {
      throw new IllegalArgumentException(
          "relationship creation and update evidence is inconsistent");
    }
    if (usedAt != null && (usedAt.isBefore(createdAt) || usedAt.isAfter(updatedAt))) {
      throw new IllegalArgumentException("code usage is outside relationship chronology");
    }
    if (decisionAt != null
        && (usedAt == null || decisionAt.isBefore(usedAt) || decisionAt.isAfter(updatedAt))) {
      throw new IllegalArgumentException("consent decision is outside relationship chronology");
    }
    if (confirmedAt != null
        && (usedAt == null || confirmedAt.isBefore(usedAt) || confirmedAt.isAfter(updatedAt))) {
      throw new IllegalArgumentException("confirmation is outside relationship chronology");
    }
    boolean consistent =
        switch (status) {
          case PENDING -> usedAt == null && decisionAt == null && confirmedAt == null;
          case AWAITING_CONSENT ->
              usedAt != null && confirmedAt == null && (decisionAt == null || consent.isGranted());
          case CONFIRMED ->
              usedAt != null
                  && consent.isGranted()
                  && confirmedAt != null
                  && !confirmedAt.isBefore(decisionAt);
          case REVOKED ->
              usedAt != null
                  && !consent.isGranted()
                  && decisionAt != null
                  && (confirmedAt == null || !decisionAt.isBefore(confirmedAt));
        };
    if (!consistent) {
      throw new IllegalArgumentException(
          "relationship status and consent evidence are inconsistent");
    }
  }

  /** Returns the relationship identity. */
  public String id() {
    return id;
  }

  /** Returns its caregiver identity without loading Identity account state. */
  public String caregiverId() {
    return caregiverId;
  }

  /** Returns its adult profile identity. */
  public String olderAdultId() {
    return olderAdultId;
  }

  /** Returns the relationship lifecycle state. */
  public CareLinkStatus status() {
    return status;
  }

  /** Returns immutable linking evidence that callers cannot mutate through the aggregate. */
  public LinkingCode linkingCode() {
    return linkingCode;
  }

  /** Returns the current recorded consent snapshot. */
  public Consent consent() {
    return consent;
  }

  /** Returns confirmation time, or null before confirmation. */
  public Instant confirmedAt() {
    return confirmedAt;
  }

  /** Returns relationship creation time. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns the last domain transition time. */
  public Instant updatedAt() {
    return updatedAt;
  }
}
