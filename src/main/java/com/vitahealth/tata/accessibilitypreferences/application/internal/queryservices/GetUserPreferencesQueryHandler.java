package com.vitahealth.tata.accessibilitypreferences.application.internal.queryservices;

import com.vitahealth.tata.accessibilitypreferences.application.internal.assemblers.UserPreferencesResultFromAggregateAssembler;
import com.vitahealth.tata.accessibilitypreferences.application.queryservices.GetUserPreferencesQueryService;
import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.vitahealth.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;
import com.vitahealth.tata.accessibilitypreferences.domain.repositories.UserPreferencesRepository;
import java.time.Clock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns read-only preference lookup and its explicit application-time default fallback. */
@Service
@Transactional(readOnly = true)
public class GetUserPreferencesQueryHandler implements GetUserPreferencesQueryService {
  private final UserPreferencesRepository repository;
  private final Clock clock;

  /** Requires persistence and the same application clock used by deliberate preference creation. */
  public GetUserPreferencesQueryHandler(UserPreferencesRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Reads stored preferences or returns defaults without creating persistence state. */
  @Override
  public UserPreferencesResult handle(GetUserPreferencesQuery query) {
    var preferences =
        repository
            .findByUserId(query.userId())
            .orElseGet(
                () -> UserPreferencesFactory.createDefaults(query.userId(), clock.instant()));
    return UserPreferencesResultFromAggregateAssembler.toResult(preferences);
  }
}
