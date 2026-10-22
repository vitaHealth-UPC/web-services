package com.vitahealth.tata.carelink.application.internal.queryservices;

import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.queries.GetConfirmedCareLinksByCaregiverQuery;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads full confirmed relationship evidence for context consumers. */
@Service
@Transactional(readOnly = true)
public class GetConfirmedCareLinksByCaregiverQueryHandler {
  private final CareLinkRepository careLinkRepository;

  /** Injects only persistence contracts required by this query. */
  public GetConfirmedCareLinksByCaregiverQueryHandler(CareLinkRepository careLinkRepository) {
    this.careLinkRepository = careLinkRepository;
  }

  /** Loads full confirmed relationship evidence for context consumers. */
  public List<CareLinkResult> handle(GetConfirmedCareLinksByCaregiverQuery query) {
    return careLinkRepository.findConfirmedByCaregiver(query.caregiverId()).stream()
        .map(CareLinkMapper::toResult)
        .toList();
  }
}
