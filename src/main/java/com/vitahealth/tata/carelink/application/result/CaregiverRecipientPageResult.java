package com.vitahealth.tata.carelink.application.result;

import java.util.List;

/** Credential-free authorized caregiver IDs and the next scanned candidate cursor. */
public record CaregiverRecipientPageResult(List<String> caregiverIds, String nextCursor) {
  /** Copies recipient IDs; a null cursor denotes the end of candidate storage. */
  public CaregiverRecipientPageResult {
    caregiverIds = List.copyOf(caregiverIds);
  }
}
