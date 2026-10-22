package com.vitahealth.tata.adherenceanalytics.domain.model.valueobjects;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/** Validated owner and half-open consolidation period with a canonical calendar identity. */
public record AdherencePeriod(String olderAdultId, Instant from, Instant to, String zone) {
  /** Normalizes identity components and preserves the supported eight-day maximum. */
  public AdherencePeriod {
    validateRange(olderAdultId, from, to);
    olderAdultId = olderAdultId.trim();
    if (zone == null || zone.isBlank())
      throw new IllegalArgumentException("calendar zone is required");
    try {
      zone = ZoneId.of(zone).normalized().getId();
    } catch (DateTimeException failure) {
      throw new IllegalArgumentException("invalid calendar zone", failure);
    }
  }

  /** Validates the shared explicit-period query bounds independently of calendar grouping. */
  public static void validateRange(String owner, Instant from, Instant to) {
    if (owner == null
        || owner.isBlank()
        || from == null
        || to == null
        || !to.isAfter(from)
        || Duration.between(from, to).compareTo(Duration.ofDays(8)) > 0) {
      throw new IllegalArgumentException("adherence range must be ordered and at most eight days");
    }
  }

  /** Returns the stable owner-period-zone key used by existing stored snapshots. */
  public String snapshotId() {
    return UUID.nameUUIDFromBytes(
            (olderAdultId + "|" + from + "|" + to + "|" + zone).getBytes(StandardCharsets.UTF_8))
        .toString();
  }
}
