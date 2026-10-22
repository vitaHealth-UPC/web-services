package com.vitahealth.tata.accessibilitypreferences.application.queryservices;

import com.vitahealth.tata.accessibilitypreferences.application.result.UserPreferencesResult;
import com.vitahealth.tata.accessibilitypreferences.domain.model.queries.GetUserPreferencesQuery;

/** Reads one user's complete preferences without initializing persistence state. */
public interface GetUserPreferencesQueryService {
  /** Returns stored state or product defaults using explicit application time. */
  UserPreferencesResult handle(GetUserPreferencesQuery query);
}
