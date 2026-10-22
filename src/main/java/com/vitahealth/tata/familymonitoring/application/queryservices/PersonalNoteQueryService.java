package com.vitahealth.tata.familymonitoring.application.queryservices;

import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetPersonalNotesQuery;
import java.util.List;

/** Reads immutable private reminder history for a validated owner query. */
public interface PersonalNoteQueryService {
  /** Returns private notes in deterministic recent-first order. */
  List<PersonalNote> handle(GetPersonalNotesQuery query);
}
