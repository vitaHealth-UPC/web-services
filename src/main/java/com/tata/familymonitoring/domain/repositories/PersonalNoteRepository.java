package com.tata.familymonitoring.domain.repositories;

import com.tata.familymonitoring.domain.model.entities.PersonalNote;
import java.util.List;

public interface PersonalNoteRepository {
  PersonalNote save(PersonalNote note);

  List<PersonalNote> findByOwner(String olderAdultId);
}
