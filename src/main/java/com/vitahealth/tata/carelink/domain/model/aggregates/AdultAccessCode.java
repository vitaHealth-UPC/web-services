package com.vitahealth.tata.carelink.domain.model.aggregates;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Single-use code a caregiver issues so the adult of a confirmed relationship can sign in on a new
 * or reinstalled device. Redeeming it identifies the adult; the adult's PIN still protects access
 * when one exists.
 */
public final class AdultAccessCode {
  /** What redeeming the code does. */
  public enum Kind {
    /** Signs the adult in on a new device. */
    ADULT_ACCESS,
    /** Lets another caregiver request to care for the same adult. */
    CAREGIVER_INVITATION
  }

  private final String id;
  private final Kind kind;
  private final String careLinkId;
  private final String olderAdultId;
  private final String issuedByCaregiverId;
  private LinkingCode code;
  private final Instant createdAt;

  private AdultAccessCode(
      String id,
      String careLinkId,
      String olderAdultId,
      String issuedByCaregiverId,
      LinkingCode code,
      Instant createdAt,
      Kind kind) {
    this.id = requireText(id, "id");
    this.kind = kind == null ? Kind.ADULT_ACCESS : kind;
    this.careLinkId = requireText(careLinkId, "careLinkId");
    this.olderAdultId = requireText(olderAdultId, "olderAdultId");
    this.issuedByCaregiverId = requireText(issuedByCaregiverId, "issuedByCaregiverId");
    this.code = Objects.requireNonNull(code, "code");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  /** Issues an unused code for the confirmed relationship. */
  public static AdultAccessCode issue(
      String careLinkId,
      String olderAdultId,
      String caregiverId,
      String value,
      Instant now,
      Duration lifetime) {
    return issue(careLinkId, olderAdultId, caregiverId, value, now, lifetime, Kind.ADULT_ACCESS);
  }

  /** Issues an unused code of the given kind. */
  public static AdultAccessCode issue(
      String careLinkId,
      String olderAdultId,
      String caregiverId,
      String value,
      Instant now,
      Duration lifetime,
      Kind kind) {
    return new AdultAccessCode(
        UUID.randomUUID().toString(),
        careLinkId,
        olderAdultId,
        caregiverId,
        LinkingCode.issue(value, now, lifetime),
        now,
        kind);
  }

  /** Restores stored evidence without renewing or consuming it. */
  public static AdultAccessCode rehydrate(
      String id,
      String careLinkId,
      String olderAdultId,
      String issuedByCaregiverId,
      LinkingCode code,
      Instant createdAt,
      Kind kind) {
    return new AdultAccessCode(
        id, careLinkId, olderAdultId, issuedByCaregiverId, code, createdAt, kind);
  }

  /** Consumes the code; it cannot be used twice nor after expiring. */
  public void redeem(Instant now) {
    code = code.useAt(now);
  }

  /** Reports whether the code can still be redeemed. */
  public boolean isValid(Instant now) {
    return code.isValid(now);
  }

  /** Returns what redeeming the code does. */
  public Kind kind() {
    return kind;
  }

  /** Returns the stable identity. */
  public String id() {
    return id;
  }

  /** Returns the confirmed relationship this code restores access to. */
  public String careLinkId() {
    return careLinkId;
  }

  /** Returns the adult the code identifies. */
  public String olderAdultId() {
    return olderAdultId;
  }

  /** Returns the caregiver who issued it. */
  public String issuedByCaregiverId() {
    return issuedByCaregiverId;
  }

  /** Returns the code evidence. */
  public LinkingCode code() {
    return code;
  }

  /** Returns the issuance instant. */
  public Instant createdAt() {
    return createdAt;
  }
}
