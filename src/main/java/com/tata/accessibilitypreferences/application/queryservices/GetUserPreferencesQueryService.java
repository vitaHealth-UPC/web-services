package com.tata.accessibilitypreferences.application.queryservices;

import com.tata.accessibilitypreferences.domain.model.aggregates.UserPreferences;
import com.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;

public interface GetUserPreferencesQueryService {
    UserPreferences handle(GetUserPreferencesQuery query);
}
