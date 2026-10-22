package com.vitahealth.tata.familymonitoring.application.queryservices;

import com.vitahealth.tata.familymonitoring.application.result.CaregiverNoteResult;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetCaregiverNotesQuery;
import java.util.List;

/** Reads interventions belonging to the requesting caregiver's current relationship. */
public interface GetCaregiverNotesQueryService {
  /** Returns immutable intervention snapshots in recent-first order. */
  List<CaregiverNoteResult> handle(GetCaregiverNotesQuery query);
}
