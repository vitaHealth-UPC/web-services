package com.vitahealth.tata.carelink.application.internal.queryservices;

import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.queries.FindCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads an optional relationship without translating persistence failures into absence. */
@Service
@Transactional(readOnly = true)
public class FindCareLinkByIdQueryHandler {
  private final CareLinkRepository relationships;

  /** Injects the context-owned persistence contract. */
  public FindCareLinkByIdQueryHandler(CareLinkRepository relationships) {
    this.relationships = relationships;
  }

  /** Preserves absence and allows technical failures to propagate to the calling boundary. */
  public Optional<CareLinkResult> handle(FindCareLinkByIdQuery query) {
    return relationships.findById(query.careLinkId()).map(CareLinkMapper::toResult);
  }
}
