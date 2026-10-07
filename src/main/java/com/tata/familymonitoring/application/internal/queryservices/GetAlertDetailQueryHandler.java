package com.tata.familymonitoring.application.internal.queryservices;

import com.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.tata.familymonitoring.domain.model.entities.AlertSummary;
import com.tata.familymonitoring.domain.model.queries.GetAlertDetailQuery;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAlertDetailQueryHandler implements com.tata.familymonitoring.application.queryservices.GetAlertDetailQueryService {

  private final IFamilyMonitorRepository repository;

  public GetAlertDetailQueryHandler(IFamilyMonitorRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public AlertSummary handle(GetAlertDetailQuery query) {
    return repository.findByOlderAdultId(query.olderAdultId())
        .orElseThrow(() -> new FamilyMonitorNotFoundException(query.olderAdultId()))
        .findAlert(query.alertId());
  }
}
