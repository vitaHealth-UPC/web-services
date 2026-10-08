package com.tata.familymonitoring.application.internal.queryservices;

import com.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.tata.familymonitoring.domain.model.queries.GetCaregiverNotesQuery;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCaregiverNotesQueryHandler implements com.tata.familymonitoring.application.queryservices.GetCaregiverNotesQueryService {

  private final IFamilyMonitorRepository repository;

  public GetCaregiverNotesQueryHandler(IFamilyMonitorRepository repository) {
    this.repository = repository;
  }

  /** Most recent notes first. */
  @Transactional(readOnly = true)
  public List<CaregiverNote> handle(GetCaregiverNotesQuery query) {
    return repository.findByOlderAdultId(query.olderAdultId())
        .orElseThrow(() -> new FamilyMonitorNotFoundException(query.olderAdultId()))
        .getNotes().stream()
        .sorted(Comparator.comparing(CaregiverNote::getRecordedAt).reversed())
        .toList();
  }
}
