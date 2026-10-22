package com.vitahealth.tata.intakeexecution.domain.model.valueobjects;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import java.time.ZoneId;

/** Explicit supported IANA calendar zone, including UTC for the legacy schedule contract. */
public record ScheduleTimeZone(String id) {
  /** Rejects unknown regions and raw offsets that cannot describe calendar transitions. */
  public ScheduleTimeZone {
    id = requireText(id, "schedule time zone");
    if (!ZoneId.getAvailableZoneIds().contains(id)) {
      throw new IllegalArgumentException("schedule time zone must be a supported IANA zone");
    }
  }

  /** Returns the zone rules used to resolve local calendar times. */
  public ZoneId zoneId() {
    return ZoneId.of(id);
  }
}
