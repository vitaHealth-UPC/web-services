package com.vitahealth.tata.familymonitoring.application.queryservices;

/** Defines the authorization check required by caregiver monitoring use cases. */
public interface CareRelationshipQueryService {
  void check(String caregiverId, String olderAdultId);
}
