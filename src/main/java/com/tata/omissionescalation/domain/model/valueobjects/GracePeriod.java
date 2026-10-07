package com.tata.omissionescalation.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Extra interval during which an intake can still be confirmed before it counts as omitted. */
public record GracePeriod(
    Instant startsAt,
    Instant endsAt) {

  public GracePeriod {
    Objects.requireNonNull(startsAt, "startsAt must not be null");
    Objects.requireNonNull(endsAt, "endsAt must not be null");
    if (endsAt.isBefore(startsAt)) {
      throw new IllegalArgumentException("endsAt must not be before startsAt");
    }
  }

  public static GracePeriod startingAt(Instant startsAt, Duration length) {
    return new GracePeriod(startsAt, startsAt.plus(length));
  }

  public boolean isActive(Instant now) {
    return !now.isBefore(startsAt) && now.isBefore(endsAt);
  }

  public boolean isExpired(Instant now) {
    return !now.isBefore(endsAt);
  }
}
