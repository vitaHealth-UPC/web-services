package com.vitahealth.tata.familymonitoring.application.internal.queryservices;

import com.vitahealth.tata.familymonitoring.application.queryservices.GetCaregiverNotesQueryService;
import com.vitahealth.tata.familymonitoring.application.result.CaregiverNoteResult;
import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetCaregiverNotesQuery;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads only the notes owned by the requesting caregiver-adult follow-up. */
@Service
public class GetCaregiverNotesQueryHandler implements GetCaregiverNotesQueryService {

  private final FamilyMonitorSelectionService selection;

  /** Injects scoped follow-up storage. */
  public GetCaregiverNotesQueryHandler(FamilyMonitorSelectionService selection) {
    this.selection = selection;
  }

  /** Most recent notes first. */
  @Override
  @Transactional(readOnly = true)
  public List<CaregiverNoteResult> handle(GetCaregiverNotesQuery query) {
    return selection.requireCurrent(query.olderAdultId(), query.caregiverId()).getNotes().stream()
        .sorted(
            Comparator.comparing(CaregiverNote::getRecordedAt)
                .thenComparing(CaregiverNote::getId)
                .reversed())
        .map(CaregiverNoteResult::from)
        .toList();
  }
}
