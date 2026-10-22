package com.vitahealth.tata.carelink.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** Immutable linking credential evidence; only the owning aggregate replaces its usage state. */
public final class LinkingCode {
  /** Storage and public transport bound for linking evidence. */
  public static final int MAX_LENGTH = 32;

  private final String value;
  private final Instant expiresAt;
  private final Instant usedAt;

  private LinkingCode(String value, Instant expiresAt, Instant usedAt) {
    this.value = requireText(value, "linking code").toUpperCase(Locale.ROOT);
    if (this.value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("linking code exceeds its storage bound");
    }
    this.expiresAt = Objects.requireNonNull(expiresAt);
    if (usedAt != null && !usedAt.isBefore(expiresAt)) {
      throw new IllegalArgumentException("linking code use must precede expiration");
    }
    this.usedAt = usedAt;
  }

  /** Issues unused evidence with a positive lifetime and locale-independent identifier. */
  public static LinkingCode issue(String value, Instant now, Duration ttl) {
    Objects.requireNonNull(now);
    Objects.requireNonNull(ttl);
    if (ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("linking code ttl must be positive");
    }
    return new LinkingCode(value, now.plus(ttl), null);
  }

  /** Restores valid evidence without renewing the code or changing its use timestamp. */
  public static LinkingCode rehydrate(String value, Instant expiresAt, Instant usedAt) {
    return new LinkingCode(value, expiresAt, usedAt);
  }

  /** Requires an unused credential strictly before its expiration. */
  public boolean isValid(Instant now) {
    Objects.requireNonNull(now);
    return usedAt == null && now.isBefore(expiresAt);
  }

  /** Returns consumed evidence, leaving previously published value snapshots unchanged. */
  public LinkingCode useAt(Instant now) {
    if (!isValid(now)) {
      throw new IllegalStateException("linking code is expired or already used");
    }
    return new LinkingCode(value, expiresAt, now);
  }

  /** Reports valid historical use even after the credential itself has expired. */
  public boolean wasUsedWithinValidity() {
    return usedAt != null && usedAt.isBefore(expiresAt);
  }

  /** Returns the normalized credential value for matching and persistence. */
  public String value() {
    return value;
  }

  /** Returns the exclusive expiration bound. */
  public Instant expiresAt() {
    return expiresAt;
  }

  /** Returns accepted usage time, or null before acceptance. */
  public Instant usedAt() {
    return usedAt;
  }
}
