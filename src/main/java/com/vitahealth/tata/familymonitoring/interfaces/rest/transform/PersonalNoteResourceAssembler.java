package com.vitahealth.tata.familymonitoring.interfaces.rest.transform;

import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.PersonalNoteResource;

/** Maps immutable private reminder evidence to its session-scoped REST representation. */
public final class PersonalNoteResourceAssembler {
  private PersonalNoteResourceAssembler() {}

  /** Copies the stored note identity, content, category and original recording instant. */
  public static PersonalNoteResource toResource(PersonalNote note) {
    return new PersonalNoteResource(
        note.id(), note.title(), note.text(), note.category(), note.recordedAt());
  }
}
