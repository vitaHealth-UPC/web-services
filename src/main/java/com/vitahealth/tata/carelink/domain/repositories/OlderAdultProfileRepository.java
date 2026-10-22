package com.vitahealth.tata.carelink.domain.repositories;

import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Stores adult profiles while keeping identity and ownership independent of JPA details. */
public interface OlderAdultProfileRepository {
  /** Inserts a profile or updates its existing managed persistence state. */
  OlderAdultProfile save(OlderAdultProfile profile);

  /** Reads a profile without reserving it for mutation. */
  Optional<OlderAdultProfile> findById(String id);

  /** Reserves the current profile until the caller's mutation transaction finishes. */
  Optional<OlderAdultProfile> findByIdForUpdate(String id);

  /** Loads a batch of profile identities without issuing one query per displayed relationship. */
  List<OlderAdultProfile> findAllByIds(Collection<String> ids);
}
