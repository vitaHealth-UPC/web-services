package com.vitahealth.tata.familymonitoring.application.internal.queryservices;

import com.vitahealth.tata.familymonitoring.application.queryservices.PersonalNoteQueryService;
import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetPersonalNotesQuery;
import com.vitahealth.tata.familymonitoring.domain.repositories.PersonalNoteRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads only the private reminder history selected by the authenticated owner query. */
@Service
public class GetPersonalNotesQueryHandler implements PersonalNoteQueryService {
  private final PersonalNoteRepository repository;

  /** Injects private owner-scoped history storage. */
  public GetPersonalNotesQueryHandler(PersonalNoteRepository repository) {
    this.repository = repository;
  }

  /** Returns immutable history in the storage contract's deterministic recent-first order. */
  @Override
  @Transactional(readOnly = true)
  public List<PersonalNote> handle(GetPersonalNotesQuery query) {
    return List.copyOf(repository.findByOwner(query.olderAdultId()));
  }
}
