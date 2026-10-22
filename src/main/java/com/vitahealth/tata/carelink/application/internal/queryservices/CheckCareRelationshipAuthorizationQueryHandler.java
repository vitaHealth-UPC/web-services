package com.vitahealth.tata.carelink.application.internal.queryservices;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.queries.CheckCareRelationshipAuthorizationQuery;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads current confirmed consent authorization for a member pair. */
@Service
@Transactional(readOnly = true)
public class CheckCareRelationshipAuthorizationQueryHandler {
  private final CareLinkRepository careLinkRepository;

  /** Injects only persistence contracts required by this query. */
  public CheckCareRelationshipAuthorizationQueryHandler(CareLinkRepository careLinkRepository) {
    this.careLinkRepository = careLinkRepository;
  }

  /** Reads current confirmed consent authorization for a member pair. */
  public boolean handle(CheckCareRelationshipAuthorizationQuery query) {
    return careLinkRepository
        .findConfirmed(query.caregiverId(), query.olderAdultId())
        .map(CareLink::isActive)
        .orElse(false);
  }
}
