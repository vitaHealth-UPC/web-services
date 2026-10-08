package com.tata.accessibilitypreferences.application.internal.queryservices;

import com.tata.accessibilitypreferences.domain.factories.UserPreferencesFactory;
import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import java.time.Instant;
import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetUserPreferencesQueryHandler implements com.tata.accessibilitypreferences.application.queryservices.GetUserPreferencesQueryService {
    private final IUserPreferencesRepository repository;

    public GetUserPreferencesQueryHandler(IUserPreferencesRepository repository) {
        this.repository = repository;
    }

    /** Reads stored preferences or returns defaults without creating persistence state. */
    public UserPreferences handle(GetUserPreferencesQuery query) {
        return repository.findByUserId(query.userId())
                .orElseGet(() -> UserPreferencesFactory.createDefaults(query.userId(), Instant.now()));
    }
}
