package com.tata.familymonitoring.interfaces.rest.transform;

import com.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.tata.familymonitoring.interfaces.rest.resources.CaregiverNoteResource;

public final class CaregiverNoteResourceFromEntityAssembler {

  private CaregiverNoteResourceFromEntityAssembler() {
  }

  public static CaregiverNoteResource toResourceFromEntity(CaregiverNote note) {
    return new CaregiverNoteResource(
        note.getId(), note.getText(), note.getRecordedAt(), note.getFamiliarId());
  }
}
