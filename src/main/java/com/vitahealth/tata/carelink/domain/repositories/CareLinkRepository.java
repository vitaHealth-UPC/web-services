package com.vitahealth.tata.carelink.domain.repositories;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persists relationship aggregates and exposes explicit reservations for lifecycle changes. */
public interface CareLinkRepository {
  /** Saves domain state; lifecycle updates first reserve the relationship in their transaction. */
  CareLink save(CareLink careLink);

  /** Reads relationship evidence without acquiring a mutation reservation. */
  Optional<CareLink> findById(String id);

  /** Reserves the current relationship until the caller's mutation transaction finishes. */
  Optional<CareLink> findByIdForUpdate(String id);

  /** Reads code evidence without reserving it for acceptance. */
  Optional<CareLink> findByCode(String code);

  /** Reports whether a candidate is reserved by an unused pending relationship. */
  boolean isCodeReserved(String code);

  /** Reserves matching code evidence until acceptance and credential issuance finish. */
  Optional<CareLink> findByCodeForUpdate(String code);

  /** Reserves live requests and confirmed relationships for the specified member pair. */
  List<CareLink> findOpenMemberPairForUpdate(String caregiverId, String olderAdultId, Instant now);

  /** Finds a confirmed relationship between the specified logical context identities. */
  Optional<CareLink> findConfirmed(String caregiverId, String olderAdultId);

  /** Lists active confirmed relationships without changing their consent evidence. */
  List<CareLink> findConfirmedByCaregiver(String caregiverId);

  /** Scans one ordered caregiver-ID page with confirmed consent for the requested adult. */
  List<String> findAuthorizedCaregiverIdsAfter(String olderAdultId, String afterId, int limit);
}
