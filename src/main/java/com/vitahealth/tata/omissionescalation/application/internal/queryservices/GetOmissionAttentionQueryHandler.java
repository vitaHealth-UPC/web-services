package com.vitahealth.tata.omissionescalation.application.internal.queryservices;

import com.vitahealth.tata.omissionescalation.application.queryservices.GetOmissionAttentionQueryService;
import com.vitahealth.tata.omissionescalation.domain.model.queries.GetOmissionAttentionQuery;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads bounded batches without loading mutable omission aggregates into another context. */
@Service
public class GetOmissionAttentionQueryHandler implements GetOmissionAttentionQueryService {
  private static final int BATCH_SIZE = 100;
  private final IOmissionCaseRepository repository;

  public GetOmissionAttentionQueryHandler(IOmissionCaseRepository repository) {
    this.repository = repository;
  }

  /**
   * Preserves missing cases as missing and validates every stored level through its value object.
   */
  @Override
  @Transactional(readOnly = true)
  public Map<String, Integer> handle(GetOmissionAttentionQuery query) {
    var ids = new ArrayList<>(query.intakeIds());
    var levels = new HashMap<String, Integer>();
    for (int start = 0; start < ids.size(); start += BATCH_SIZE) {
      var batch = Set.copyOf(ids.subList(start, Math.min(start + BATCH_SIZE, ids.size())));
      repository
          .findAttentionLevels(query.olderAdultId(), batch)
          .forEach(
              (id, level) -> {
                if (!batch.contains(id))
                  throw new IllegalStateException("unexpected attention reference");
                levels.put(id, level.value());
              });
    }
    return Map.copyOf(levels);
  }
}
