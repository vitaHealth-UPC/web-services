package com.vitahealth.tata.omissionescalation.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Extra interval during which an intake can still be confirmed before it counts as omitted. */
public record GracePeriod(Instant startsAt, Instant endsAt) {

  /** Requires explicit ordered boundaries; equal boundaries represent immediate expiry. */
  public GracePeriod {
    Objects.requireNonNull(startsAt, "startsAt must not be null");
    Objects.requireNonNull(endsAt, "endsAt must not be null");
    if (endsAt.isBefore(startsAt)) {
      throw new IllegalArgumentException("endsAt must not be before startsAt");
    }
  }

  /**
   * Anchors the entire interval to the scheduled source occurrence, without using wall-clock time.
   */
  public static GracePeriod startingAt(Instant startsAt, Duration length) {
    return new GracePeriod(startsAt, startsAt.plus(length));
  }

  /** Includes the start and excludes the end of the confirmation grace interval. */
  public boolean isActive(Instant now) {
    return !now.isBefore(startsAt) && now.isBefore(endsAt);
  }

  /** Reports expiry at the end boundary and for every later instant. */
  public boolean isExpired(Instant now) {
    return !now.isBefore(endsAt);
  }
}
