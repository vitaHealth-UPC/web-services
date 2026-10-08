package com.tata.familymonitoring.application.queryservices;

import com.tata.familymonitoring.domain.model.entities.PersonalNote;
import java.util.List;

public interface PersonalNoteQueryService {
  List<PersonalNote> list(String olderAdultId);
}
