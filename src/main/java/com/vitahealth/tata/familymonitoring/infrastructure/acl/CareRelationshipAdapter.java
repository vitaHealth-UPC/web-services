package com.vitahealth.tata.familymonitoring.infrastructure.acl;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.familymonitoring.domain.ports.CareRelationshipPort;
import org.springframework.stereotype.Component;

/** Reads current relationship authorization exclusively through Care Link's public facade. */
@Component
public class CareRelationshipAdapter implements CareRelationshipPort {
  private final CareLinkContextFacade queries;

  /** Injects the public relationship query boundary. */
  public CareRelationshipAdapter(CareLinkContextFacade queries) {
    this.queries = queries;
  }

  /** Requires the exact source relationship and both participants to remain consented. */
  @Override
  public boolean isRelationshipAuthorized(
      String careLinkId, String caregiverId, String olderAdultId) {
    return queries
        .findById(careLinkId)
        .filter(
            link ->
                link.caregiverId().equals(caregiverId)
                    && link.olderAdultId().equals(olderAdultId)
                    && link.status() == CareLinkStatus.CONFIRMED
                    && link.consentGranted())
        .isPresent();
  }

  @Override
  public boolean isAuthorized(String caregiverId, String olderAdultId) {
    return queries.isAuthorized(caregiverId, olderAdultId);
  }
}
