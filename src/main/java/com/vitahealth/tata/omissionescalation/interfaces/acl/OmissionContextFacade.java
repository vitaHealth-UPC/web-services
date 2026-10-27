package com.vitahealth.tata.omissionescalation.interfaces.acl;

import com.vitahealth.tata.omissionescalation.application.queryservices.GetOmissionAttentionQueryService;
import com.vitahealth.tata.omissionescalation.domain.model.queries.GetOmissionAttentionQuery;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Public read contract; consumers authorize their caregiver scope before requesting evidence. */
@Component
public class OmissionContextFacade {
  private final GetOmissionAttentionQueryService attention;

  public OmissionContextFacade(GetOmissionAttentionQueryService attention) {
    this.attention = attention;
  }

  /**
   * Returns the highest recorded level per requested intake of this adult; no case means no entry.
   */
  public Map<String, Integer> escalationLevels(String olderAdultId, Set<String> intakeIds) {
    return attention.handle(new GetOmissionAttentionQuery(olderAdultId, intakeIds));
  }
}
