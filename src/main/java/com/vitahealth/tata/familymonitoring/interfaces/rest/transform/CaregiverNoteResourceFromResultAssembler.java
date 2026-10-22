package com.vitahealth.tata.familymonitoring.interfaces.rest.transform;

import com.vitahealth.tata.familymonitoring.application.result.CaregiverNoteResult;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.CaregiverNoteResource;

/** Maps immutable application evidence to the existing REST resource contract. */
public final class CaregiverNoteResourceFromResultAssembler {

  private CaregiverNoteResourceFromResultAssembler() {}

  /** Copies response fields without authorization, persistence or business transitions. */
  public static CaregiverNoteResource toResourceFromResult(CaregiverNoteResult note) {
    return new CaregiverNoteResource(note.id(), note.text(), note.recordedAt(), note.familiarId());
  }
}
