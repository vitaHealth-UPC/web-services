package com.vitahealth.tata.familymonitoring.application.internal.queryservices;

import com.vitahealth.tata.familymonitoring.application.queryservices.GetAlertDetailQueryService;
import com.vitahealth.tata.familymonitoring.application.result.AlertResult;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetAlertDetailQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operates on alert evidence within the selected caregiver-adult follow-up. */
@Service
public class GetAlertDetailQueryHandler implements GetAlertDetailQueryService {

  private final FamilyMonitorSelectionService selection;
  private final AlertResultProjection projection;

  /** Injects the context-owned follow-up repository. */
  public GetAlertDetailQueryHandler(
      FamilyMonitorSelectionService selection, AlertResultProjection projection) {
    this.selection = selection;
    this.projection = projection;
  }

  /** Reads the requested alert only from its caregiver-owned follow-up. */
  @Override
  @Transactional(readOnly = true)
  public AlertResult handle(GetAlertDetailQuery query) {
    return projection.project(
        query.olderAdultId(),
        selection
            .requireCurrent(query.olderAdultId(), query.caregiverId())
            .findAlert(query.alertId()));
  }
}
