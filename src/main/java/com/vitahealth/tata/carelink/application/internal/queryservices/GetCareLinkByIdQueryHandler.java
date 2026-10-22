package com.vitahealth.tata.carelink.application.internal.queryservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.queries.GetCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads one required relationship projection. */
@Service
@Transactional(readOnly = true)
public class GetCareLinkByIdQueryHandler {
  private final CareLinkRepository careLinkRepository;

  /** Injects only persistence contracts required by this query. */
  public GetCareLinkByIdQueryHandler(CareLinkRepository careLinkRepository) {
    this.careLinkRepository = careLinkRepository;
  }

  /** Loads one required relationship projection. */
  public CareLinkResult handle(GetCareLinkByIdQuery query) {
    return careLinkRepository
        .findById(query.careLinkId())
        .map(CareLinkMapper::toResult)
        .orElseThrow(
            () ->
                new CareLinkApplicationException(
                    CareLinkApplicationException.Code.CARE_LINK_NOT_FOUND, "care link not found"));
  }
}
