package com.vitahealth.tata.carelink.application.internal.queryservices;

import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.queries.FindOlderAdultProfileByIdQuery;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads optional profile evidence without hiding technical failures. */
@Service
@Transactional(readOnly = true)
public class FindOlderAdultProfileByIdQueryHandler {
  private final OlderAdultProfileRepository olderAdultRepository;

  /** Injects only persistence contracts required by this query. */
  public FindOlderAdultProfileByIdQueryHandler(OlderAdultProfileRepository olderAdultRepository) {
    this.olderAdultRepository = olderAdultRepository;
  }

  /** Reads optional profile evidence without hiding technical failures. */
  public Optional<OlderAdultProfileResult> handle(FindOlderAdultProfileByIdQuery query) {
    return olderAdultRepository.findById(query.olderAdultId()).map(CareLinkMapper::toResult);
  }
}
