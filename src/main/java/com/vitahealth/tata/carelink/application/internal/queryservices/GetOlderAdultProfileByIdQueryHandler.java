package com.vitahealth.tata.carelink.application.internal.queryservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.queries.GetOlderAdultProfileByIdQuery;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads one required adult profile projection. */
@Service
@Transactional(readOnly = true)
public class GetOlderAdultProfileByIdQueryHandler {
  private final OlderAdultProfileRepository olderAdultRepository;

  /** Injects only persistence contracts required by this query. */
  public GetOlderAdultProfileByIdQueryHandler(OlderAdultProfileRepository olderAdultRepository) {
    this.olderAdultRepository = olderAdultRepository;
  }

  /** Loads one required adult profile projection. */
  public OlderAdultProfileResult handle(GetOlderAdultProfileByIdQuery query) {
    return olderAdultRepository
        .findById(query.olderAdultId())
        .map(CareLinkMapper::toResult)
        .orElseThrow(
            () ->
                new CareLinkApplicationException(
                    CareLinkApplicationException.Code.OLDER_ADULT_NOT_FOUND,
                    "older adult profile not found"));
  }
}
