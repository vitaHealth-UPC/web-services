package com.vitahealth.tata.adherenceanalytics.domain.model.queries;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

/** Selects stored period evidence within its adult owner boundary. */
public record GetAdherenceSnapshotQuery(String olderAdultId, String snapshotId) {
  /** Requires both logical references before consulting storage. */
  public GetAdherenceSnapshotQuery {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    snapshotId = requireText(snapshotId, "snapshotId");
  }
}
