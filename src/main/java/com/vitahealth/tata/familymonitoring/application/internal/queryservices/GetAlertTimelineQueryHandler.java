package com.vitahealth.tata.familymonitoring.application.internal.queryservices;

import com.vitahealth.tata.familymonitoring.application.queryservices.GetAlertDetailQueryService;
import com.vitahealth.tata.familymonitoring.application.queryservices.GetAlertTimelineQueryService;
import com.vitahealth.tata.familymonitoring.application.result.RecordedAlertEventResult;
import com.vitahealth.tata.familymonitoring.application.result.RecordedAlertEventResult.Type;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetAlertDetailQuery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Projects immutable schedule/open/close evidence without inferring a reminder was sent. */
@Service
@Transactional(readOnly = true)
public class GetAlertTimelineQueryHandler implements GetAlertTimelineQueryService {
  private final GetAlertDetailQueryService detail;

  public GetAlertTimelineQueryHandler(GetAlertDetailQueryService detail) {
    this.detail = detail;
  }

  @Override
  public List<RecordedAlertEventResult> handle(GetAlertDetailQuery query) {
    var alert = detail.handle(query);
    var events = new ArrayList<RecordedAlertEventResult>();
    events.add(new RecordedAlertEventResult(Type.INTAKE_SCHEDULED, alert.scheduledAt()));
    events.add(new RecordedAlertEventResult(Type.ALERT_GENERATED, alert.openedAt()));
    if (alert.closedAt() != null)
      events.add(new RecordedAlertEventResult(Type.ALERT_CLOSED, alert.closedAt()));
    return List.copyOf(events);
  }
}
