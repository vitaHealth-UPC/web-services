package com.vitahealth.tata.familymonitoring.domain.repositories;

import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;
import java.util.List;

/** Appends private adult reminders and reads only the requested owner history. */
public interface PersonalNoteRepository {
  /** Inserts a new note; an existing stored identity must never be overwritten. */
  PersonalNote append(PersonalNote note);

  /** Returns owner notes by recording instant and stored identity, both descending. */
  List<PersonalNote> findByOwner(String olderAdultId);
}
