package com.vitahealth.tata.omissionescalation.application.queryservices;

import com.vitahealth.tata.omissionescalation.domain.model.queries.GetOmissionAttentionQuery;
import java.util.Map;

/** Published read-only query of recorded escalation levels, from zero through three. */
public interface GetOmissionAttentionQueryService {
  Map<String, Integer> handle(GetOmissionAttentionQuery query);
}
