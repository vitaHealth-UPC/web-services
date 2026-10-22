package com.vitahealth.tata.familymonitoring.application.internal;

import com.vitahealth.tata.familymonitoring.application.CareRelationshipRequiredException;
import com.vitahealth.tata.familymonitoring.application.queryservices.CareRelationshipQueryService;
import com.vitahealth.tata.familymonitoring.domain.ports.CareRelationshipPort;
import org.springframework.stereotype.Service;

/** Requires current consented caregiver access before monitoring queries proceed. */
@Service
public class RequireCareRelationship implements CareRelationshipQueryService {
  private final CareRelationshipPort relationships;

  /** Injects the public relationship verification port rather than another context repository. */
  public RequireCareRelationship(CareRelationshipPort relationships) {
    this.relationships = relationships;
  }

  /**
   * Rejects missing actor selectors and absent consented access without masking source failures.
   */
  @Override
  public void check(String caregiverId, String olderAdultId) {
    if (caregiverId == null
        || caregiverId.isBlank()
        || olderAdultId == null
        || olderAdultId.isBlank())
      throw new IllegalArgumentException("caregiverId and olderAdultId are required");
    if (!relationships.isAuthorized(caregiverId.trim(), olderAdultId.trim()))
      throw new CareRelationshipRequiredException();
  }
}
