package com.vitahealth.tata.carelink.application.internal.queryservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.result.ConfirmedCareLinkResult;
import com.vitahealth.tata.carelink.domain.model.queries.GetConfirmedCareLinkSummariesQuery;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads credential-free display summaries through batched profile reads. */
@Service
@Transactional(readOnly = true)
public class GetConfirmedCareLinkSummariesQueryHandler {
  private final CareLinkRepository careLinkRepository;
  private final OlderAdultProfileRepository olderAdultRepository;

  /** Injects only persistence contracts required by this query. */
  public GetConfirmedCareLinkSummariesQueryHandler(
      CareLinkRepository careLinkRepository, OlderAdultProfileRepository olderAdultRepository) {
    this.careLinkRepository = careLinkRepository;
    this.olderAdultRepository = olderAdultRepository;
  }

  /** Loads credential-free display summaries through batched profile reads. */
  public List<ConfirmedCareLinkResult> handle(GetConfirmedCareLinkSummariesQuery query) {
    var confirmed = careLinkRepository.findConfirmedByCaregiver(query.caregiverId());
    if (confirmed.isEmpty()) return List.of();
    var profiles =
        olderAdultRepository
            .findAllByIds(confirmed.stream().map(link -> link.olderAdultId()).distinct().toList())
            .stream()
            .collect(Collectors.toMap(profile -> profile.id(), Function.identity()));
    return confirmed.stream()
        .map(
            link -> {
              var profile = profiles.get(link.olderAdultId());
              if (profile == null)
                throw new CareLinkApplicationException(
                    CareLinkApplicationException.Code.OLDER_ADULT_NOT_FOUND,
                    "older adult profile not found");
              return new ConfirmedCareLinkResult(
                  link.id(),
                  link.olderAdultId(),
                  profile.basicData().fullName(),
                  link.confirmedAt());
            })
        .toList();
  }
}
