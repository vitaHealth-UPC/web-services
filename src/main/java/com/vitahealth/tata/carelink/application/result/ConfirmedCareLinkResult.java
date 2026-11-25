package com.vitahealth.tata.carelink.application.result;

import java.time.Instant;

/** Application list projection containing display data without linking or session credentials. */
public record ConfirmedCareLinkResult(
    String id,
    String olderAdultId,
    String olderAdultName,
    Instant confirmedAt,
    boolean adultSignedIn) {
  /** Builds a summary whose device state is unknown. */
  public ConfirmedCareLinkResult(
      String id, String olderAdultId, String olderAdultName, Instant confirmedAt) {
    this(id, olderAdultId, olderAdultName, confirmedAt, false);
  }

  /** Omits the adult's display name from diagnostic output across context boundaries. */
  @Override
  public String toString() {
    return "ConfirmedCareLinkResult[id=" + id + ", personalData=REDACTED]";
  }
}
