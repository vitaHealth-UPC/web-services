package com.tata.familymonitoring.application.queryservices;

import com.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.tata.familymonitoring.domain.model.queries.GetCaregiverNotesQuery;
import java.util.List;

public interface GetCaregiverNotesQueryService {
    List<CaregiverNote> handle(GetCaregiverNotesQuery query);
}
