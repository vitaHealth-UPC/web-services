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

  /** Injects the context-owned follow-up repository. */
  public GetAlertDetailQueryHandler(FamilyMonitorSelectionService selection) {
    this.selection = selection;
  }

  /** Reads the requested alert only from its caregiver-owned follow-up. */
  @Override
  @Transactional(readOnly = true)
  public AlertResult handle(GetAlertDetailQuery query) {
    return AlertResult.from(
        selection
            .requireCurrent(query.olderAdultId(), query.caregiverId())
            .findAlert(query.alertId()));
  }
}
