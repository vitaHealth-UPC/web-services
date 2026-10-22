package com.vitahealth.tata.carelink.application.internal.queryservices;

import com.vitahealth.tata.carelink.application.result.CaregiverRecipientPageResult;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.queries.GetAuthorizedCaregiversByOlderAdultQuery;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves bounded caregiver candidates through validated current domain consent evidence. */
@Service
@Transactional(readOnly = true)
public class GetAuthorizedCaregiversByOlderAdultQueryHandler {
  private final CareLinkRepository relationships;

  /** Injects relationship reads without depending on Identity or notification transport. */
  public GetAuthorizedCaregiversByOlderAdultQueryHandler(CareLinkRepository relationships) {
    this.relationships = relationships;
  }

  /**
   * Advances past candidates that lose authorization without replaying relationship transitions.
   */
  public CaregiverRecipientPageResult handle(GetAuthorizedCaregiversByOlderAdultQuery query) {
    var candidates =
        relationships.findAuthorizedCaregiverIdsAfter(
            query.olderAdultId(), query.afterId(), query.limit());
    var authorized = new ArrayList<String>();
    for (String caregiver : candidates)
      if (relationships
          .findConfirmed(caregiver, query.olderAdultId())
          .map(CareLink::isActive)
          .orElse(false)) authorized.add(caregiver);
    return new CaregiverRecipientPageResult(
        authorized, candidates.size() == query.limit() ? candidates.getLast() : null);
  }
}
