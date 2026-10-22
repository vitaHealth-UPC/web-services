package com.vitahealth.tata.familymonitoring.application.result;

import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import java.time.Instant;

/** Immutable persisted caregiver intervention returned by application services. */
public record CaregiverNoteResult(Long id, String text, Instant recordedAt, String familiarId) {
  /** Copies original evidence without retaining a mutable domain entity. */
  public static CaregiverNoteResult from(CaregiverNote source) {
    return new CaregiverNoteResult(
        source.getId(), source.getText(), source.getRecordedAt(), source.getFamiliarId());
  }
}
