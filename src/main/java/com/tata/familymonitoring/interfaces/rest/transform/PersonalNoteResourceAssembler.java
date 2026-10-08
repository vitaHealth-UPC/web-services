package com.tata.familymonitoring.interfaces.rest.transform;

import com.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.tata.familymonitoring.interfaces.rest.resources.PersonalNoteResource;

public final class PersonalNoteResourceAssembler {
  private PersonalNoteResourceAssembler() {}

  public static PersonalNoteResource toResource(PersonalNote note) {
    return new PersonalNoteResource(
        note.id(), note.title(), note.text(), note.category(), note.recordedAt());
  }
}
